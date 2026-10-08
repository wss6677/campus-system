package com.ivy.campus.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ivy.campus.common.BizException;
import com.ivy.campus.common.PageResult;
import com.ivy.campus.entity.Notice;
import com.ivy.campus.mapper.NoticeMapper;
import com.ivy.campus.security.UserContext;
import com.ivy.campus.service.NoticeService;
import com.ivy.campus.util.PageUtils;
import com.ivy.campus.vo.NoticeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoticeServiceImpl implements NoticeService {

    private final NoticeMapper noticeMapper;

    @Override
    public PageResult<NoticeVO> myNotices(Integer pageNum, Integer pageSize, Integer readFlag) {
        Long userId = requireUser();
        IPage<Notice> page = noticeMapper.selectPage(PageUtils.of(pageNum, pageSize),
                Wrappers.<Notice>lambdaQuery()
                        .eq(Notice::getUserId, userId)
                        .eq(readFlag != null, Notice::getReadFlag, readFlag)
                        .orderByDesc(Notice::getCreateTime)
                        .orderByDesc(Notice::getId));
        List<NoticeVO> records = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public Long unreadCount() {
        Long userId = requireUser();
        return noticeMapper.selectCount(Wrappers.<Notice>lambdaQuery()
                .eq(Notice::getUserId, userId)
                .eq(Notice::getReadFlag, 0));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void read(Long id) {
        Long userId = requireUser();
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BizException("通知不存在");
        }
        if (!userId.equals(notice.getUserId())) {
            throw new BizException("无权操作他人通知");
        }
        Notice update = new Notice();
        update.setId(id);
        update.setReadFlag(1);
        noticeMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void readAll() {
        Long userId = requireUser();
        Notice update = new Notice();
        update.setReadFlag(1);
        noticeMapper.update(update, Wrappers.<Notice>lambdaUpdate()
                .eq(Notice::getUserId, userId)
                .eq(Notice::getReadFlag, 0));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void send(Long userId, String title, String content, String type, Long bizId) {
        if (userId == null) {
            return;
        }
        noticeMapper.insert(build(userId, title, content, type, bizId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendBatch(List<Long> userIds, String title, String content, String type, Long bizId) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        List<Long> distinct = userIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        for (Long userId : distinct) {
            noticeMapper.insert(build(userId, title, content, type, bizId));
        }
    }

    private Notice build(Long userId, String title, String content, String type, Long bizId) {
        Notice notice = new Notice();
        notice.setUserId(userId);
        notice.setTitle(title);
        notice.setContent(content);
        notice.setType(type == null || type.isBlank() ? "SYSTEM" : type);
        notice.setBizId(bizId);
        notice.setReadFlag(0);
        notice.setCreateTime(LocalDateTime.now());
        return notice;
    }

    private NoticeVO toVO(Notice notice) {
        NoticeVO vo = new NoticeVO();
        BeanUtils.copyProperties(notice, vo);
        return vo;
    }

    private Long requireUser() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BizException("未登录或登录已过期");
        }
        return userId;
    }
}
