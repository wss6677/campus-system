package com.ivy.campus.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ivy.campus.common.BizException;
import com.ivy.campus.entity.Announcement;
import com.ivy.campus.entity.AnnouncementScope;
import com.ivy.campus.entity.Notice;
import com.ivy.campus.entity.Receipt;
import com.ivy.campus.entity.User;
import com.ivy.campus.mapper.AnnouncementMapper;
import com.ivy.campus.mapper.AnnouncementScopeMapper;
import com.ivy.campus.mapper.NoticeMapper;
import com.ivy.campus.mapper.ReceiptMapper;
import com.ivy.campus.mapper.UserMapper;
import com.ivy.campus.security.LoginUser;
import com.ivy.campus.security.UserContext;
import com.ivy.campus.service.ReceiptService;
import com.ivy.campus.vo.DeptUnreadVO;
import com.ivy.campus.vo.ReceiptVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReceiptServiceImpl implements ReceiptService {

    private static final String VISIBILITY_ALL = "ALL";

    private static final String SCOPE_USER = "USER";

    private static final String TYPE_URGE = "URGE";

    private final ReceiptMapper receiptMapper;
    private final AnnouncementMapper announcementMapper;
    private final AnnouncementScopeMapper announcementScopeMapper;
    private final NoticeMapper noticeMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long announcementId) {
        LoginUser current = requireLogin();
        Announcement announcement = announcementMapper.selectById(announcementId);
        if (announcement == null) {
            throw new BizException("公告不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        Receipt exists = receiptMapper.selectOne(Wrappers.<Receipt>lambdaQuery()
                .eq(Receipt::getAnnouncementId, announcementId)
                .eq(Receipt::getUserId, current.getUserId())
                .last("limit 1"));
        if (exists == null) {
            Receipt receipt = new Receipt();
            receipt.setAnnouncementId(announcementId);
            receipt.setUserId(current.getUserId());
            receipt.setDeptId(current.getDeptId());
            receipt.setReadFlag(1);
            receipt.setReadTime(now);
            receipt.setReadDuration(0);
            receipt.setUrgeCount(0);
            receipt.setCreateTime(now);
            receiptMapper.insert(receipt);
            increaseReceiptCount(announcement, 1);
            return;
        }
        if (exists.getReadFlag() != null && exists.getReadFlag() == 1) {
            return;
        }
        Receipt update = new Receipt();
        update.setId(exists.getId());
        update.setReadFlag(1);
        update.setReadTime(now);
        receiptMapper.updateById(update);
        increaseReceiptCount(announcement, 1);
    }

    @Override
    public ReceiptVO statByAnnouncement(Long announcementId) {
        Announcement announcement = announcementMapper.selectById(announcementId);
        if (announcement == null) {
            throw new BizException("公告不存在");
        }
        ReceiptVO vo = new ReceiptVO();
        vo.setAnnouncementId(announcementId);
        Long total = receiptMapper.selectCount(Wrappers.<Receipt>lambdaQuery()
                .eq(Receipt::getAnnouncementId, announcementId));
        Long readCount = receiptMapper.selectCount(Wrappers.<Receipt>lambdaQuery()
                .eq(Receipt::getAnnouncementId, announcementId)
                .eq(Receipt::getReadFlag, 1));
        long totalValue = total == null ? 0L : total;
        long readValue = readCount == null ? 0L : readCount;
        if (totalValue == 0 && announcement.getTargetCount() != null) {
            totalValue = announcement.getTargetCount();
        }
        vo.setTotal(totalValue);
        vo.setReadCount(readValue);
        vo.setUnreadCount(Math.max(totalValue - readValue, 0L));
        vo.setRate(totalValue <= 0 ? 0.0D : Math.round(readValue * 10000.0D / totalValue) / 100.0D);
        vo.setDeptRanks(unreadRank(announcementId));
        return vo;
    }

    @Override
    public List<DeptUnreadVO> unreadRank(Long announcementId) {
        List<DeptUnreadVO> list = receiptMapper.selectUnreadRank(announcementId);
        return list == null ? new ArrayList<>() : list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long urge(Long announcementId, Long deptId) {
        Announcement announcement = announcementMapper.selectById(announcementId);
        if (announcement == null) {
            throw new BizException("公告不存在");
        }
        List<Receipt> unreadList = receiptMapper.selectList(Wrappers.<Receipt>lambdaQuery()
                .eq(Receipt::getAnnouncementId, announcementId)
                .eq(Receipt::getReadFlag, 0)
                .eq(deptId != null, Receipt::getDeptId, deptId));
        if (unreadList.isEmpty()) {
            return 0L;
        }
        LocalDateTime now = LocalDateTime.now();
        int urgeCount = 0;
        for (Receipt receipt : unreadList) {
            Receipt update = new Receipt();
            update.setId(receipt.getId());
            update.setUrgeCount(receipt.getUrgeCount() == null ? 1 : receipt.getUrgeCount() + 1);
            update.setLastUrgeTime(now);
            receiptMapper.updateById(update);
            Notice notice = new Notice();
            notice.setUserId(receipt.getUserId());
            notice.setTitle("公告阅读催办");
            notice.setContent("请及时阅读《" + announcement.getTitle() + "》");
            notice.setType(TYPE_URGE);
            notice.setBizId(announcementId);
            notice.setReadFlag(0);
            notice.setCreateTime(now);
            noticeMapper.insert(notice);
            urgeCount++;
        }
        log.info("公告催办完成，announcementId={}，催办人数={}", announcementId, urgeCount);
        return (long) urgeCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generateTargets(Long announcementId) {
        Announcement announcement = announcementMapper.selectById(announcementId);
        if (announcement == null) {
            throw new BizException("公告不存在");
        }
        List<Receipt> exists = receiptMapper.selectList(Wrappers.<Receipt>lambdaQuery()
                .eq(Receipt::getAnnouncementId, announcementId));
        if (!exists.isEmpty()) {
            return exists.size();
        }
        List<User> targets = resolveTargetUsers(announcement);
        LocalDateTime now = LocalDateTime.now();
        int count = 0;
        for (User user : targets) {
            Receipt receipt = new Receipt();
            receipt.setAnnouncementId(announcementId);
            receipt.setUserId(user.getId());
            receipt.setDeptId(user.getDeptId());
            receipt.setReadFlag(0);
            receipt.setReadDuration(0);
            receipt.setUrgeCount(0);
            receipt.setCreateTime(now);
            receiptMapper.insert(receipt);
            count++;
        }
        log.info("生成回执目标行完成，announcementId={}，目标人数={}", announcementId, count);
        return count;
    }

    private List<User> resolveTargetUsers(Announcement announcement) {
        String visibility = announcement.getVisibility() == null ? VISIBILITY_ALL : announcement.getVisibility();
        if (VISIBILITY_ALL.equals(visibility)) {
            return userMapper.selectList(Wrappers.<User>lambdaQuery().eq(User::getStatus, 1));
        }
        Set<Long> deptIds = new LinkedHashSet<>();
        Set<Long> userIds = new LinkedHashSet<>();
        if (announcement.getDeptId() != null) {
            deptIds.add(announcement.getDeptId());
        }
        if (announcement.getId() != null) {
            List<AnnouncementScope> scopes = announcementScopeMapper.selectList(
                    Wrappers.<AnnouncementScope>lambdaQuery()
                            .eq(AnnouncementScope::getAnnouncementId, announcement.getId()));
            for (AnnouncementScope scope : scopes) {
                if (scope.getScopeValue() == null) {
                    continue;
                }
                if (SCOPE_USER.equals(scope.getScopeType())) {
                    userIds.add(scope.getScopeValue());
                } else {
                    deptIds.add(scope.getScopeValue());
                }
            }
        }
        List<User> result = new ArrayList<>();
        if (!deptIds.isEmpty()) {
            result.addAll(userMapper.selectList(Wrappers.<User>lambdaQuery()
                    .eq(User::getStatus, 1)
                    .in(User::getDeptId, deptIds)));
        }
        for (Long userId : userIds) {
            User user = userMapper.selectById(userId);
            if (user == null || (user.getStatus() != null && user.getStatus() == 0)) {
                continue;
            }
            boolean exists = result.stream().anyMatch(item -> item.getId().equals(user.getId()));
            if (!exists) {
                result.add(user);
            }
        }
        return result;
    }

    private void increaseReceiptCount(Announcement announcement, int delta) {
        try {
            announcementMapper.update(null, Wrappers.<Announcement>lambdaUpdate()
                    .eq(Announcement::getId, announcement.getId())
                    .setSql("receipt_count = receipt_count + " + delta));
        } catch (Exception e) {
            log.warn("回执数自增失败，announcementId={}，原因：{}", announcement.getId(), e.getMessage());
        }
    }

    private LoginUser requireLogin() {
        LoginUser loginUser = UserContext.get();
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new BizException("未登录或登录已过期");
        }
        return loginUser;
    }
}
