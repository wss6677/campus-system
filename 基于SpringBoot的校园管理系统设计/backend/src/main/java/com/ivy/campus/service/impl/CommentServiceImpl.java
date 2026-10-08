package com.ivy.campus.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ivy.campus.common.BizException;
import com.ivy.campus.common.PageResult;
import com.ivy.campus.dto.CommentDTO;
import com.ivy.campus.entity.Announcement;
import com.ivy.campus.entity.Comment;
import com.ivy.campus.entity.Dept;
import com.ivy.campus.entity.User;
import com.ivy.campus.mapper.AnnouncementMapper;
import com.ivy.campus.mapper.CommentMapper;
import com.ivy.campus.mapper.DeptMapper;
import com.ivy.campus.mapper.UserMapper;
import com.ivy.campus.security.LoginUser;
import com.ivy.campus.security.UserContext;
import com.ivy.campus.service.CommentService;
import com.ivy.campus.util.PageUtils;
import com.ivy.campus.vo.CommentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentServiceImpl implements CommentService {

    private static final String STATUS_PENDING = "PENDING";

    private static final String STATUS_APPROVED = "APPROVED";

    private static final String STATUS_REJECTED = "REJECTED";

    private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";

    private static final String ROLE_UNIV_AUDITOR = "UNIV_AUDITOR";

    private final CommentMapper commentMapper;
    private final AnnouncementMapper announcementMapper;
    private final UserMapper userMapper;
    private final DeptMapper deptMapper;

    @Override
    public PageResult<CommentVO> pageByAnnouncement(Long announcementId, Integer pageNum, Integer pageSize) {
        if (announcementId == null) {
            throw new BizException("公告ID不能为空");
        }
        IPage<Comment> page = commentMapper.selectPage(PageUtils.of(pageNum, pageSize),
                Wrappers.<Comment>lambdaQuery()
                        .eq(Comment::getAnnouncementId, announcementId)
                        .eq(Comment::getStatus, STATUS_APPROVED)
                        .and(wrapper -> wrapper.isNull(Comment::getParentId).or().eq(Comment::getParentId, 0L))
                        .orderByDesc(Comment::getCreateTime)
                        .orderByDesc(Comment::getId));
        List<Comment> records = page.getRecords() == null ? new ArrayList<>() : page.getRecords();
        List<CommentVO> voList = new ArrayList<>();
        Map<Long, User> userMap = loadUserMap(records);
        for (Comment item : records) {
            CommentVO vo = toVO(item, userMap);
            List<Comment> children = commentMapper.selectList(Wrappers.<Comment>lambdaQuery()
                    .eq(Comment::getAnnouncementId, announcementId)
                    .eq(Comment::getParentId, item.getId())
                    .eq(Comment::getStatus, STATUS_APPROVED)
                    .orderByAsc(Comment::getCreateTime));
            for (Comment child : children) {
                vo.getChildren().add(toVO(child, loadUserMap(children)));
            }
            voList.add(vo);
        }
        return PageResult.of(voList, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long add(CommentDTO dto) {
        LoginUser current = requireLogin();
        Announcement announcement = announcementMapper.selectById(dto.getAnnouncementId());
        if (announcement == null) {
            throw new BizException("公告不存在");
        }
        if (announcement.getAllowComment() != null && announcement.getAllowComment() == 0) {
            throw new BizException("该公告已关闭评论");
        }
        Comment comment = new Comment();
        comment.setAnnouncementId(dto.getAnnouncementId());
        comment.setUserId(current.getUserId());
        comment.setParentId(dto.getParentId() == null ? 0L : dto.getParentId());
        comment.setContent(dto.getContent());
        comment.setLikeCount(0);
        comment.setStatus(STATUS_APPROVED);
        comment.setCreateTime(LocalDateTime.now());
        commentMapper.insert(comment);
        log.info("评论发布成功，id={}，announcementId={}", comment.getId(), dto.getAnnouncementId());
        return comment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, String status) {
        Comment comment = requireComment(id);
        String target = status == null || status.isBlank() ? STATUS_APPROVED : status.toUpperCase();
        if (!STATUS_APPROVED.equals(target) && !STATUS_REJECTED.equals(target) && !STATUS_PENDING.equals(target)) {
            throw new BizException("评论审核状态非法");
        }
        LoginUser current = requireLogin();
        Comment update = new Comment();
        update.setId(comment.getId());
        update.setStatus(target);
        update.setAuditUserId(current.getUserId());
        commentMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        Comment comment = requireComment(id);
        LoginUser current = requireLogin();
        boolean privileged = ROLE_SUPER_ADMIN.equals(current.getRoleCode())
                || ROLE_UNIV_AUDITOR.equals(current.getRoleCode());
        if (!privileged && !current.getUserId().equals(comment.getUserId())) {
            throw new BizException("无权删除他人评论");
        }
        commentMapper.deleteById(id);
    }

    private Comment requireComment(Long id) {
        if (id == null) {
            throw new BizException("评论ID不能为空");
        }
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BizException("评论不存在");
        }
        return comment;
    }

    private CommentVO toVO(Comment comment, Map<Long, User> userMap) {
        CommentVO vo = new CommentVO();
        BeanUtils.copyProperties(comment, vo);
        vo.setStatusText(statusText(comment.getStatus()));
        if (vo.getLikeCount() == null) {
            vo.setLikeCount(0);
        }
        User user = userMap == null || comment.getUserId() == null ? null : userMap.get(comment.getUserId());
        if (user == null && comment.getUserId() != null) {
            user = userMapper.selectById(comment.getUserId());
        }
        if (user != null) {
            vo.setUserName(user.getRealName() == null || user.getRealName().isBlank()
                    ? user.getUsername() : user.getRealName());
            vo.setUserAvatar(user.getAvatar());
            if (user.getDeptId() != null) {
                Dept dept = deptMapper.selectById(user.getDeptId());
                if (dept != null) {
                    vo.setDeptName(dept.getName());
                }
            }
        }
        return vo;
    }

    private Map<Long, User> loadUserMap(List<Comment> records) {
        Map<Long, User> map = new HashMap<>();
        if (records == null || records.isEmpty()) {
            return map;
        }
        List<Long> ids = records.stream().map(Comment::getUserId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return map;
        }
        for (User user : userMapper.selectBatchIds(ids)) {
            map.put(user.getId(), user);
        }
        return map;
    }

    private String statusText(String status) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case STATUS_PENDING:
                return "待审核";
            case STATUS_APPROVED:
                return "已通过";
            case STATUS_REJECTED:
                return "已驳回";
            default:
                return status;
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
