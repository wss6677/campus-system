package com.ivy.campus.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ivy.campus.common.BizException;
import com.ivy.campus.common.Constants;
import com.ivy.campus.common.PageResult;
import com.ivy.campus.dto.AnnouncementDTO;
import com.ivy.campus.dto.AnnouncementQuery;
import com.ivy.campus.dto.AuditDTO;
import com.ivy.campus.entity.Announcement;
import com.ivy.campus.entity.AnnouncementAttachment;
import com.ivy.campus.entity.AnnouncementScope;
import com.ivy.campus.entity.AnnouncementTagRel;
import com.ivy.campus.entity.AnnouncementVersion;
import com.ivy.campus.entity.ApprovalLog;
import com.ivy.campus.entity.Category;
import com.ivy.campus.entity.Comment;
import com.ivy.campus.entity.Dept;
import com.ivy.campus.entity.Tag;
import com.ivy.campus.entity.User;
import com.ivy.campus.mapper.AnnouncementAttachmentMapper;
import com.ivy.campus.mapper.AnnouncementMapper;
import com.ivy.campus.mapper.AnnouncementScopeMapper;
import com.ivy.campus.mapper.AnnouncementTagRelMapper;
import com.ivy.campus.mapper.AnnouncementVersionMapper;
import com.ivy.campus.mapper.ApprovalLogMapper;
import com.ivy.campus.mapper.CategoryMapper;
import com.ivy.campus.mapper.CommentMapper;
import com.ivy.campus.mapper.DeptMapper;
import com.ivy.campus.mapper.TagMapper;
import com.ivy.campus.mapper.UserMapper;
import com.ivy.campus.security.LoginUser;
import com.ivy.campus.security.UserContext;
import com.ivy.campus.service.AnnouncementService;
import com.ivy.campus.service.NoticeService;
import com.ivy.campus.service.PushService;
import com.ivy.campus.service.ReceiptService;
import com.ivy.campus.util.PageUtils;
import com.ivy.campus.util.SensitiveWordUtil;
import com.ivy.campus.vo.AnnouncementDetailVO;
import com.ivy.campus.vo.AnnouncementVO;
import com.ivy.campus.vo.ApprovalLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnnouncementServiceImpl implements AnnouncementService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_SCHEDULED = "SCHEDULED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_WITHDRAWN = "WITHDRAWN";
    private static final String STATUS_ARCHIVED = "ARCHIVED";

    private static final String VISIBILITY_ALL = "ALL";
    private static final String VISIBILITY_COLLEGE = "COLLEGE";
    private static final String VISIBILITY_CLASS = "CLASS";
    private static final String VISIBILITY_CUSTOM = "CUSTOM";

    private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    private static final String ROLE_UNIV_AUDITOR = "UNIV_AUDITOR";
    private static final String ROLE_DEPT_PUBLISHER = "DEPT_PUBLISHER";
    private static final String ROLE_TEACHER = "TEACHER";
    private static final String ROLE_STUDENT = "STUDENT";

    private static final String NODE_SUBMIT = "SUBMIT";
    private static final String NODE_DEPT_AUDIT = "DEPT_AUDIT";
    private static final String NODE_UNIV_AUDIT = "UNIV_AUDIT";
    private static final String NODE_PUBLISH = "PUBLISH";
    private static final String NODE_REJECT = "REJECT";
    private static final String NODE_WITHDRAW = "WITHDRAW";
    private static final String NODE_ARCHIVE = "ARCHIVE";
    private static final String NODE_UPDATE = "UPDATE";

    private static final String TYPE_ANNOUNCE = "ANNOUNCE";
    private static final String TYPE_AUDIT = "AUDIT";

    private final AnnouncementMapper announcementMapper;
    private final AnnouncementAttachmentMapper attachmentMapper;
    private final AnnouncementScopeMapper scopeMapper;
    private final AnnouncementTagRelMapper tagRelMapper;
    private final TagMapper tagMapper;
    private final AnnouncementVersionMapper versionMapper;
    private final ApprovalLogMapper approvalLogMapper;
    private final CommentMapper commentMapper;
    private final UserMapper userMapper;
    private final DeptMapper deptMapper;
    private final CategoryMapper categoryMapper;
    private final NoticeService noticeService;
    private final ReceiptService receiptService;
    private final PushService pushService;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public PageResult<AnnouncementVO> page(AnnouncementQuery query) {
        AnnouncementQuery condition = query == null ? new AnnouncementQuery() : query;
        LoginUser current = UserContext.get();
        boolean publishOnly = false;
        if (current != null) {
            String role = current.getRoleCode();
            if (ROLE_DEPT_PUBLISHER.equals(role)) {
                if (current.getDeptId() != null) {
                    condition.setDeptId(current.getDeptId());
                }
            } else if (ROLE_TEACHER.equals(role) || ROLE_STUDENT.equals(role)) {
                condition.setStatus(STATUS_PUBLISHED);
                publishOnly = true;
            }
        }
        IPage<Announcement> page = announcementMapper.selectAnnouncementPage(
                PageUtils.of(condition.getPageNum(), condition.getPageSize()), condition);
        List<Announcement> records = page.getRecords() == null ? new ArrayList<>() : page.getRecords();
        if (publishOnly && !records.isEmpty()) {
            records = records.stream()
                    .filter(item -> visibleByScope(item, current))
                    .collect(Collectors.toList());
        }
        return PageResult.of(fillList(records), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public AnnouncementDetailVO detail(Long id, boolean countView) {
        Announcement announcement = requireAnnouncement(id);
        LoginUser current = UserContext.get();
        if (announcement.getStatus() != null && !STATUS_PUBLISHED.equals(announcement.getStatus())) {
            boolean privileged = current != null && (ROLE_SUPER_ADMIN.equals(current.getRoleCode())
                    || ROLE_UNIV_AUDITOR.equals(current.getRoleCode())
                    || ROLE_DEPT_PUBLISHER.equals(current.getRoleCode())
                    || (current.getUserId() != null && current.getUserId().equals(announcement.getAuthorId())));
            if (!privileged) {
                throw new BizException("公告尚未发布");
            }
        } else if (!visibleByScope(announcement, current)
                && (current == null || !(ROLE_SUPER_ADMIN.equals(current.getRoleCode())
                || ROLE_UNIV_AUDITOR.equals(current.getRoleCode())
                || ROLE_DEPT_PUBLISHER.equals(current.getRoleCode())))) {
            throw new BizException("无权查看该公告");
        }
        if (countView) {
            try {
                announcementMapper.update(null, Wrappers.<Announcement>lambdaUpdate()
                        .eq(Announcement::getId, id)
                        .setSql("view_count = view_count + 1"));
                announcement.setViewCount(announcement.getViewCount() == null ? 1 : announcement.getViewCount() + 1);
            } catch (Exception e) {
                log.warn("浏览量自增失败：{}", e.getMessage());
            }
        }
        AnnouncementDetailVO vo = new AnnouncementDetailVO();
        BeanUtils.copyProperties(fillOne(announcement, null, null, null, null), vo);
        vo.setContent(announcement.getContent());
        vo.setKeywords(announcement.getKeywords());
        vo.setAuditRemark(announcement.getAuditRemark());
        if (announcement.getAuditUserId() != null) {
            User auditor = userMapper.selectById(announcement.getAuditUserId());
            if (auditor != null) {
                vo.setAuditUserName(auditor.getRealName());
            }
        }
        vo.setAttachments(attachmentMapper.selectList(Wrappers.<AnnouncementAttachment>lambdaQuery()
                .eq(AnnouncementAttachment::getAnnouncementId, id)
                .orderByAsc(AnnouncementAttachment::getId)));
        vo.setScopes(scopeMapper.selectList(Wrappers.<AnnouncementScope>lambdaQuery()
                .eq(AnnouncementScope::getAnnouncementId, id)
                .orderByAsc(AnnouncementScope::getId)));
        vo.setTags(loadTags(id));
        vo.setApprovalLogs(listApprovalLogs(id));
        int target = announcement.getTargetCount() == null ? 0 : announcement.getTargetCount();
        int receipt = announcement.getReceiptCount() == null ? 0 : announcement.getReceiptCount();
        vo.setReceiptRate(target <= 0 ? 0.0D : Math.round(receipt * 10000.0D / target) / 100.0D);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveDraft(AnnouncementDTO dto) {
        LoginUser current = requireLogin();
        Announcement announcement = new Announcement();
        applyDto(announcement, dto, false);
        announcement.setAuthorId(current.getUserId());
        announcement.setStatus(STATUS_DRAFT);
        announcement.setDeptId(announcement.getDeptId() == null ? current.getDeptId() : announcement.getDeptId());
        announcement.setCreateBy(current.getUserId());
        announcement.setViewCount(0);
        announcement.setReceiptCount(0);
        announcement.setTargetCount(0);
        announcement.setVersion(1);
        announcement.setCreateTime(LocalDateTime.now());
        announcement.setUpdateTime(LocalDateTime.now());
        String blockWord = needBlockCheck(dto);
        if (blockWord != null) {
            throw new BizException("正文包含敏感词：" + blockWord);
        }
        announcementMapper.insert(announcement);
        saveRelations(announcement.getId(), dto);
        saveVersion(announcement, "新建草稿");
        log.info("公告草稿保存成功，id={}, title={}", announcement.getId(), announcement.getTitle());
        return announcement.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AnnouncementDTO dto) {
        Announcement announcement = requireAnnouncement(id);
        if (!STATUS_DRAFT.equals(announcement.getStatus()) && !STATUS_REJECTED.equals(announcement.getStatus())) {
            throw new BizException("仅草稿或被驳回的公告允许编辑");
        }
        LoginUser current = requireLogin();
        if (!canOperate(current, announcement)) {
            throw new BizException("无权编辑他人公告");
        }
        String blockWord = needBlockCheck(dto);
        if (blockWord != null) {
            throw new BizException("正文包含敏感词：" + blockWord);
        }
        applyDto(announcement, dto, true);
        announcement.setVersion(announcement.getVersion() == null ? 2 : announcement.getVersion() + 1);
        announcement.setUpdateTime(LocalDateTime.now());
        announcementMapper.updateById(announcement);
        saveRelations(id, dto);
        saveVersion(announcement, "编辑公告内容");
        writeLog(id, NODE_UPDATE, current, "编辑公告", "内容已更新", announcement.getStatus(), announcement.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long id) {
        Announcement announcement = requireAnnouncement(id);
        String from = announcement.getStatus();
        if (!STATUS_DRAFT.equals(from) && !STATUS_REJECTED.equals(from)) {
            throw new BizException("仅草稿或被驳回的公告允许提交审核");
        }
        LoginUser current = requireLogin();
        if (!canOperate(current, announcement)) {
            throw new BizException("无权提交他人公告");
        }
        String content = joinText(announcement.getTitle(), announcement.getContent(), announcement.getSummary());
        if (SensitiveWordUtil.hasBlock(content)) {
            throw new BizException("正文命中阻断级敏感词：" + String.join("、", SensitiveWordUtil.hit(content)));
        }
        if (SensitiveWordUtil.hasWarn(content)) {
            log.warn("公告命中提示级敏感词：{}，announcementId={}", SensitiveWordUtil.hit(content), id);
        }
        Category category = announcement.getCategoryId() == null ? null : categoryMapper.selectById(announcement.getCategoryId());
        boolean needAudit = category == null || category.getNeedAudit() == null || category.getNeedAudit() == 1;
        if (!needAudit) {
            announcement.setAuditUserId(current.getUserId());
            announcement.setAuditTime(LocalDateTime.now());
            announcement.setAuditRemark("分类免审自动通过");
            announcement.setUpdateTime(LocalDateTime.now());
            announcementMapper.updateById(announcement);
            writeLog(id, NODE_PUBLISH, current, "免审自动发布", "分类免审自动通过", from, STATUS_PUBLISHED);
            doPublish(announcement);
            noticeService.send(announcement.getAuthorId(), "公告已发布", "《" + announcement.getTitle() + "》已免审发布", TYPE_ANNOUNCE, id);
            return;
        }
        announcement.setStatus(STATUS_PENDING);
        announcement.setUpdateTime(LocalDateTime.now());
        announcementMapper.updateById(announcement);
        writeLog(id, NODE_SUBMIT, current, "提交审核", "提交至校级审核", from, STATUS_PENDING);
        noticeService.sendBatch(listUserIdsByRole(ROLE_UNIV_AUDITOR), "待审核公告",
                "《" + announcement.getTitle() + "》等待审核", TYPE_AUDIT, id);
        noticeService.send(announcement.getAuthorId(), "公告已提交审核",
                "《" + announcement.getTitle() + "》已提交审核", TYPE_AUDIT, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, AuditDTO dto) {
        Announcement announcement = requireAnnouncement(id);
        String from = announcement.getStatus();
        if (!STATUS_PENDING.equals(from)) {
            throw new BizException("仅待审核状态的公告可以审核");
        }
        if (dto.getRemark() == null || dto.getRemark().isBlank()) {
            throw new BizException("请填写审批意见");
        }
        LoginUser current = requireLogin();
        announcement.setAuditUserId(current.getUserId());
        announcement.setAuditTime(LocalDateTime.now());
        announcement.setAuditRemark(dto.getRemark());
        boolean pass = dto.getPass() != null && dto.getPass();
        if (!pass) {
            announcement.setStatus(STATUS_REJECTED);
            announcement.setUpdateTime(LocalDateTime.now());
            announcementMapper.updateById(announcement);
            writeLog(id, NODE_REJECT, current, "审核驳回",
                    dto.getRemark() == null || dto.getRemark().isBlank() ? "审核未通过" : dto.getRemark(),
                    from, STATUS_REJECTED);
            noticeService.send(announcement.getAuthorId(), "公告被驳回",
                    "《" + announcement.getTitle() + "》审核未通过：" + safeText(dto.getRemark()), TYPE_AUDIT, id);
            return;
        }
        LocalDateTime plan = dto.getPublishTime() == null ? announcement.getPublishTime() : dto.getPublishTime();
        if (plan != null && plan.isAfter(LocalDateTime.now())) {
            announcement.setPublishTime(plan);
            announcement.setStatus(STATUS_SCHEDULED);
            announcement.setUpdateTime(LocalDateTime.now());
            announcementMapper.updateById(announcement);
            writeLog(id, NODE_UNIV_AUDIT, current, "审核通过并定时发布",
                    "计划发布时间 " + plan, from, STATUS_SCHEDULED);
            noticeService.send(announcement.getAuthorId(), "公告审核通过",
                    "《" + announcement.getTitle() + "》审核通过，将于 " + plan + " 自动发布", TYPE_AUDIT, id);
            return;
        }
        announcement.setPublishTime(plan == null ? LocalDateTime.now() : plan);
        announcement.setStatus(STATUS_PUBLISHED);
        announcement.setUpdateTime(LocalDateTime.now());
        announcementMapper.updateById(announcement);
        writeLog(id, NODE_UNIV_AUDIT, current, "审核通过", "审核通过并立即发布", from, STATUS_PUBLISHED);
        doPublish(announcement);
        noticeService.send(announcement.getAuthorId(), "公告审核通过",
                "《" + announcement.getTitle() + "》审核通过并已发布", TYPE_AUDIT, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        Announcement announcement = requireAnnouncement(id);
        String from = announcement.getStatus();
        if (STATUS_PUBLISHED.equals(from)) {
            throw new BizException("公告已处于发布状态");
        }
        if (STATUS_ARCHIVED.equals(from)) {
            throw new BizException("公告已归档，不允许发布");
        }
        LoginUser current = requireLogin();
        if (!canOperate(current, announcement)) {
            throw new BizException("无权发布他人公告");
        }
        announcement.setStatus(STATUS_PUBLISHED);
        announcement.setPublishTime(LocalDateTime.now());
        announcement.setUpdateTime(LocalDateTime.now());
        announcementMapper.updateById(announcement);
        writeLog(id, NODE_PUBLISH, current, "直接发布", "跳过审核直接发布", from, STATUS_PUBLISHED);
        doPublish(announcement);
        noticeService.send(announcement.getAuthorId(), "公告已发布", "《" + announcement.getTitle() + "》已发布", TYPE_ANNOUNCE, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(Long id) {
        Announcement announcement = requireAnnouncement(id);
        String from = announcement.getStatus();
        if (!STATUS_PUBLISHED.equals(from)) {
            throw new BizException("仅已发布公告可以撤回");
        }
        LoginUser current = requireLogin();
        if (!canOperate(current, announcement)) {
            throw new BizException("无权撤回他人公告");
        }
        announcement.setStatus(STATUS_WITHDRAWN);
        announcement.setUpdateTime(LocalDateTime.now());
        announcementMapper.updateById(announcement);
        writeLog(id, NODE_WITHDRAW, current, "撤回公告", "公告已撤回", from, STATUS_WITHDRAWN);
        noticeService.send(announcement.getAuthorId(), "公告已撤回", "《" + announcement.getTitle() + "》已撤回", TYPE_ANNOUNCE, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archive(Long id) {
        Announcement announcement = requireAnnouncement(id);
        String from = announcement.getStatus();
        if (STATUS_ARCHIVED.equals(from)) {
            throw new BizException("公告已归档");
        }
        LoginUser current = requireLogin();
        archiveInternal(announcement, current, "手动归档");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleTop(Long id) {
        Announcement announcement = requireAnnouncement(id);
        LoginUser current = requireLogin();
        if (!canOperate(current, announcement)) {
            throw new BizException("无权操作他人公告");
        }
        Integer flag = announcement.getIsTop() != null && announcement.getIsTop() == 1 ? 0 : 1;
        Announcement update = new Announcement();
        update.setId(id);
        update.setIsTop(flag);
        update.setUpdateTime(LocalDateTime.now());
        announcementMapper.updateById(update);
        announcement.setIsTop(flag);
        writeLog(id, NODE_UPDATE, current, flag == 1 ? "设置置顶" : "取消置顶",
                "置顶标记变更为" + flag, announcement.getStatus(), announcement.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleBanner(Long id) {
        Announcement announcement = requireAnnouncement(id);
        LoginUser current = requireLogin();
        if (!canOperate(current, announcement)) {
            throw new BizException("无权操作他人公告");
        }
        Integer flag = announcement.getIsBanner() != null && announcement.getIsBanner() == 1 ? 0 : 1;
        Announcement update = new Announcement();
        update.setId(id);
        update.setIsBanner(flag);
        update.setUpdateTime(LocalDateTime.now());
        announcementMapper.updateById(update);
        announcement.setIsBanner(flag);
        writeLog(id, NODE_UPDATE, current, flag == 1 ? "设置轮播" : "取消轮播",
                "轮播标记变更为" + flag, announcement.getStatus(), announcement.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        Announcement announcement = requireAnnouncement(id);
        LoginUser current = requireLogin();
        if (!canOperate(current, announcement)) {
            throw new BizException("无权删除他人公告");
        }
        announcementMapper.deleteById(id);
        writeLog(id, NODE_UPDATE, current, "删除公告", "公告已逻辑删除", announcement.getStatus(), announcement.getStatus());
        clearStatsCache();
    }

    @Override
    public List<AnnouncementVO> latest(Integer limit) {
        int size = limit == null || limit <= 0 ? 10 : Math.min(limit, 100);
        List<Announcement> list = announcementMapper.selectList(Wrappers.<Announcement>lambdaQuery()
                .eq(Announcement::getStatus, STATUS_PUBLISHED)
                .orderByDesc(Announcement::getPublishTime)
                .orderByDesc(Announcement::getId)
                .last("limit " + size));
        return fillList(list);
    }

    @Override
    public List<AnnouncementVO> banner() {
        List<Announcement> list = announcementMapper.selectList(Wrappers.<Announcement>lambdaQuery()
                .eq(Announcement::getStatus, STATUS_PUBLISHED)
                .eq(Announcement::getIsBanner, 1)
                .orderByDesc(Announcement::getIsTop)
                .orderByDesc(Announcement::getPublishTime)
                .last("limit 10"));
        return fillList(list);
    }

    // 定时任务：每分钟处理到期发布与到期归档
    @Scheduled(cron = "0 */1 * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void scheduledPublishAndArchive() {
        List<Announcement> publishableList = announcementMapper.selectPublishableList();
        if (publishableList != null && !publishableList.isEmpty()) {
            for (Announcement announcement : publishableList) {
                try {
                    announcement.setStatus(STATUS_PUBLISHED);
                    if (announcement.getPublishTime() == null) {
                        announcement.setPublishTime(LocalDateTime.now());
                    }
                    announcement.setUpdateTime(LocalDateTime.now());
                    announcementMapper.updateById(announcement);
                    writeSystemLog(announcement.getId(), NODE_PUBLISH, "定时发布", "到期自动发布", STATUS_SCHEDULED, STATUS_PUBLISHED);
                    doPublish(announcement);
                    noticeService.send(announcement.getAuthorId(), "公告已发布",
                            "《" + announcement.getTitle() + "》已按计划发布", TYPE_ANNOUNCE, announcement.getId());
                } catch (Exception e) {
                    log.error("定时发布失败，announcementId={}", announcement.getId(), e);
                }
            }
        }
        List<Announcement> expiredList = announcementMapper.selectList(Wrappers.<Announcement>lambdaQuery()
                .eq(Announcement::getStatus, STATUS_PUBLISHED)
                .isNotNull(Announcement::getExpireTime)
                .le(Announcement::getExpireTime, LocalDateTime.now()));
        if (expiredList != null && !expiredList.isEmpty()) {
            for (Announcement announcement : expiredList) {
                try {
                    archiveInternal(announcement, null, "到期自动归档");
                } catch (Exception e) {
                    log.error("定时归档失败，announcementId={}", announcement.getId(), e);
                }
            }
        }
    }

    // 发布动作：回执目标、推送日志、统计缓存
    protected void doPublish(Announcement announcement) {
        Long id = announcement.getId();
        int target = 0;
        if (announcement.getNeedReceipt() != null && announcement.getNeedReceipt() == 1) {
            target = receiptService.generateTargets(id);
        }
        Announcement update = new Announcement();
        update.setId(id);
        update.setStatus(STATUS_PUBLISHED);
        update.setTargetCount(target);
        update.setReceiptCount(0);
        update.setUpdateTime(LocalDateTime.now());
        announcementMapper.updateById(update);
        announcement.setStatus(STATUS_PUBLISHED);
        announcement.setTargetCount(target);
        try {
            pushService.push(announcement);
        } catch (Exception e) {
            log.warn("公告推送失败，announcementId={}，原因：{}", id, e.getMessage());
        }
        clearStatsCache();
        log.info("公告发布完成，id={}，标题={}，目标人数={}", id, announcement.getTitle(), target);
    }

    private void archiveInternal(Announcement announcement, LoginUser current, String remark) {
        String from = announcement.getStatus();
        Announcement update = new Announcement();
        update.setId(announcement.getId());
        update.setStatus(STATUS_ARCHIVED);
        update.setUpdateTime(LocalDateTime.now());
        announcementMapper.updateById(update);
        if (current == null) {
            writeSystemLog(announcement.getId(), NODE_ARCHIVE, "归档公告", remark, from, STATUS_ARCHIVED);
        } else {
            writeLog(announcement.getId(), NODE_ARCHIVE, current, "归档公告", remark, from, STATUS_ARCHIVED);
        }
        noticeService.send(announcement.getAuthorId(), "公告已归档",
                "《" + announcement.getTitle() + "》已归档", TYPE_ANNOUNCE, announcement.getId());
        clearStatsCache();
    }

    private void applyDto(Announcement announcement, AnnouncementDTO dto, boolean keepId) {
        if (!keepId) {
            announcement.setId(null);
        }
        announcement.setTitle(dto.getTitle());
        announcement.setSubtitle(dto.getSubtitle());
        announcement.setSummary(dto.getSummary() == null || dto.getSummary().isBlank()
                ? buildSummary(dto.getContent()) : dto.getSummary());
        announcement.setContent(dto.getContent());
        announcement.setCoverImage(dto.getCoverImage());
        announcement.setCategoryId(dto.getCategoryId());
        announcement.setDeptId(dto.getDeptId());
        announcement.setCampus(dto.getCampus());
        announcement.setVisibility(dto.getVisibility() == null || dto.getVisibility().isBlank()
                ? VISIBILITY_ALL : dto.getVisibility());
        announcement.setPriority(dto.getPriority() == null || dto.getPriority().isBlank()
                ? "NORMAL" : dto.getPriority());
        announcement.setIsTop(dto.getIsTop() == null ? 0 : dto.getIsTop());
        announcement.setIsBanner(dto.getIsBanner() == null ? 0 : dto.getIsBanner());
        announcement.setIsRedHead(dto.getIsRedHead() == null ? 0 : dto.getIsRedHead());
        announcement.setAllowComment(dto.getAllowComment() == null ? 1 : dto.getAllowComment());
        announcement.setNeedReceipt(dto.getNeedReceipt() == null ? 0 : dto.getNeedReceipt());
        announcement.setPublishTime(dto.getPublishTime());
        announcement.setExpireTime(dto.getExpireTime());
        announcement.setKeywords(dto.getKeywords());
    }

    private String needBlockCheck(AnnouncementDTO dto) {
        String text = joinText(dto.getTitle(), dto.getContent(), dto.getSummary());
        List<String> hits = SensitiveWordUtil.hit(text);
        if (hits.isEmpty()) {
            return null;
        }
        return SensitiveWordUtil.hasBlock(text) ? String.join("、", hits) : null;
    }

    private void saveRelations(Long announcementId, AnnouncementDTO dto) {
        attachmentMapper.delete(Wrappers.<AnnouncementAttachment>lambdaQuery()
                .eq(AnnouncementAttachment::getAnnouncementId, announcementId));
        if (dto.getAttachments() != null && !dto.getAttachments().isEmpty()) {
            for (AnnouncementAttachment attachment : dto.getAttachments()) {
                if (attachment == null) {
                    continue;
                }
                attachment.setId(null);
                attachment.setAnnouncementId(announcementId);
                attachment.setDownloadCount(attachment.getDownloadCount() == null ? 0 : attachment.getDownloadCount());
                attachment.setCreateTime(LocalDateTime.now());
                attachmentMapper.insert(attachment);
            }
        }
        scopeMapper.delete(Wrappers.<AnnouncementScope>lambdaQuery()
                .eq(AnnouncementScope::getAnnouncementId, announcementId));
        if (dto.getScopeList() != null && !dto.getScopeList().isEmpty()) {
            for (AnnouncementScope scope : dto.getScopeList()) {
                if (scope == null) {
                    continue;
                }
                scope.setId(null);
                scope.setAnnouncementId(announcementId);
                scope.setCreateTime(LocalDateTime.now());
                scopeMapper.insert(scope);
            }
        }
        tagRelMapper.delete(Wrappers.<AnnouncementTagRel>lambdaQuery()
                .eq(AnnouncementTagRel::getAnnouncementId, announcementId));
        for (Tag tag : resolveTags(dto.getTagNames())) {
            AnnouncementTagRel rel = new AnnouncementTagRel();
            rel.setAnnouncementId(announcementId);
            rel.setTagId(tag.getId());
            tagRelMapper.insert(rel);
        }
    }

    private List<Tag> resolveTags(List<String> tagNames) {
        List<Tag> result = new ArrayList<>();
        if (tagNames == null || tagNames.isEmpty()) {
            return result;
        }
        Set<String> names = new LinkedHashSet<>();
        for (String name : tagNames) {
            if (name != null && !name.isBlank()) {
                names.add(name.trim());
            }
        }
        for (String name : names) {
            Tag tag = tagMapper.selectOne(Wrappers.<Tag>lambdaQuery().eq(Tag::getName, name).last("limit 1"));
            if (tag == null) {
                tag = new Tag();
                tag.setName(name);
                tag.setColor("#1890ff");
                tag.setUseCount(1);
                tag.setCreateTime(LocalDateTime.now());
                tagMapper.insert(tag);
            } else {
                Tag update = new Tag();
                update.setId(tag.getId());
                update.setUseCount(tag.getUseCount() == null ? 1 : tag.getUseCount() + 1);
                tagMapper.updateById(update);
            }
            result.add(tag);
        }
        return result;
    }

    private List<Tag> loadTags(Long announcementId) {
        List<AnnouncementTagRel> rels = tagRelMapper.selectList(Wrappers.<AnnouncementTagRel>lambdaQuery()
                .eq(AnnouncementTagRel::getAnnouncementId, announcementId));
        if (rels.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> tagIds = rels.stream().map(AnnouncementTagRel::getTagId).filter(java.util.Objects::nonNull)
                .distinct().collect(Collectors.toList());
        if (tagIds.isEmpty()) {
            return new ArrayList<>();
        }
        return tagMapper.selectList(Wrappers.<Tag>lambdaQuery().in(Tag::getId, tagIds));
    }

    private void saveVersion(Announcement announcement, String changeLog) {
        try {
            AnnouncementVersion version = new AnnouncementVersion();
            version.setAnnouncementId(announcement.getId());
            version.setVersionNo(announcement.getVersion() == null ? 1 : announcement.getVersion());
            version.setTitle(announcement.getTitle());
            version.setContent(announcement.getContent());
            version.setEditorId(announcement.getAuthorId());
            version.setEditorName(resolveUserName(announcement.getAuthorId()));
            version.setChangeLog(changeLog);
            version.setCreateTime(LocalDateTime.now());
            versionMapper.insert(version);
        } catch (Exception e) {
            log.warn("保存公告版本快照失败，announcementId={}，原因：{}", announcement.getId(), e.getMessage());
        }
    }

    private void writeLog(Long announcementId, String node, LoginUser operator, String action, String remark,
                          String fromStatus, String toStatus) {
        ApprovalLog approvalLog = new ApprovalLog();
        approvalLog.setAnnouncementId(announcementId);
        approvalLog.setNode(node);
        approvalLog.setOperatorId(operator == null ? null : operator.getUserId());
        approvalLog.setOperatorName(resolveUserName(operator == null ? null : operator.getUserId()));
        approvalLog.setAction(action);
        approvalLog.setRemark(remark);
        approvalLog.setFromStatus(fromStatus);
        approvalLog.setToStatus(toStatus);
        approvalLog.setCreateTime(LocalDateTime.now());
        approvalLogMapper.insert(approvalLog);
    }

    private void writeSystemLog(Long announcementId, String node, String action, String remark,
                                String fromStatus, String toStatus) {
        ApprovalLog approvalLog = new ApprovalLog();
        approvalLog.setAnnouncementId(announcementId);
        approvalLog.setNode(node);
        approvalLog.setOperatorId(0L);
        approvalLog.setOperatorName("系统");
        approvalLog.setAction(action);
        approvalLog.setRemark(remark);
        approvalLog.setFromStatus(fromStatus);
        approvalLog.setToStatus(toStatus);
        approvalLog.setCreateTime(LocalDateTime.now());
        approvalLogMapper.insert(approvalLog);
    }

    private List<ApprovalLogVO> listApprovalLogs(Long announcementId) {
        List<ApprovalLog> logs = approvalLogMapper.selectList(Wrappers.<ApprovalLog>lambdaQuery()
                .eq(ApprovalLog::getAnnouncementId, announcementId)
                .orderByAsc(ApprovalLog::getCreateTime)
                .orderByAsc(ApprovalLog::getId));
        List<ApprovalLogVO> result = new ArrayList<>();
        for (ApprovalLog item : logs) {
            ApprovalLogVO vo = new ApprovalLogVO();
            BeanUtils.copyProperties(item, vo);
            vo.setNodeText(nodeText(item.getNode()));
            result.add(vo);
        }
        return result;
    }

    private List<AnnouncementVO> fillList(List<Announcement> records) {
        List<AnnouncementVO> result = new ArrayList<>();
        if (records == null || records.isEmpty()) {
            return result;
        }
        Map<Long, Category> categoryMap = loadCategoryMap(records);
        Map<Long, User> userMap = loadUserMap(records);
        Map<Long, Dept> deptMap = loadDeptMap(records);
        Map<Long, Long> commentMap = loadCommentMap(records);
        for (Announcement item : records) {
            result.add(fillOne(item, categoryMap, userMap, deptMap, commentMap));
        }
        return result;
    }

    private AnnouncementVO fillOne(Announcement item, Map<Long, Category> categoryMap, Map<Long, User> userMap,
                                   Map<Long, Dept> deptMap, Map<Long, Long> commentMap) {
        AnnouncementVO vo = new AnnouncementVO();
        BeanUtils.copyProperties(item, vo);
        vo.setStatusText(statusText(item.getStatus()));
        Category category = categoryMap == null || item.getCategoryId() == null
                ? null : categoryMap.get(item.getCategoryId());
        if (category == null && item.getCategoryId() != null) {
            category = categoryMapper.selectById(item.getCategoryId());
        }
        if (category != null) {
            vo.setCategoryName(category.getName());
            vo.setCategoryColor(category.getColor());
        }
        User author = userMap == null || item.getAuthorId() == null ? null : userMap.get(item.getAuthorId());
        if (author == null && item.getAuthorId() != null) {
            author = userMapper.selectById(item.getAuthorId());
        }
        if (author != null) {
            vo.setAuthorName(author.getRealName() == null || author.getRealName().isBlank()
                    ? author.getUsername() : author.getRealName());
        }
        Dept dept = deptMap == null || item.getDeptId() == null ? null : deptMap.get(item.getDeptId());
        if (dept == null && item.getDeptId() != null) {
            dept = deptMapper.selectById(item.getDeptId());
        }
        if (dept != null) {
            vo.setDeptName(dept.getName());
        }
        Long commentCount = commentMap == null || item.getId() == null ? null : commentMap.get(item.getId());
        if (commentCount == null) {
            commentCount = countComment(item.getId());
        }
        vo.setCommentCount(commentCount);
        if (vo.getViewCount() == null) {
            vo.setViewCount(0);
        }
        if (vo.getReceiptCount() == null) {
            vo.setReceiptCount(0);
        }
        if (vo.getTargetCount() == null) {
            vo.setTargetCount(0);
        }
        return vo;
    }

    private Map<Long, Category> loadCategoryMap(List<Announcement> records) {
        List<Long> ids = records.stream().map(Announcement::getCategoryId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        Map<Long, Category> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        for (Category category : categoryMapper.selectBatchIds(ids)) {
            map.put(category.getId(), category);
        }
        return map;
    }

    private Map<Long, User> loadUserMap(List<Announcement> records) {
        List<Long> ids = records.stream().map(Announcement::getAuthorId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        Map<Long, User> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        for (User user : userMapper.selectBatchIds(ids)) {
            map.put(user.getId(), user);
        }
        return map;
    }

    private Map<Long, Dept> loadDeptMap(List<Announcement> records) {
        List<Long> ids = records.stream().map(Announcement::getDeptId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        Map<Long, Dept> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        for (Dept dept : deptMapper.selectBatchIds(ids)) {
            map.put(dept.getId(), dept);
        }
        return map;
    }

    private Map<Long, Long> loadCommentMap(List<Announcement> records) {
        Map<Long, Long> map = new HashMap<>();
        List<Long> ids = records.stream().map(Announcement::getId).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return map;
        }
        for (Long id : ids) {
            map.put(id, countComment(id));
        }
        return map;
    }

    private Long countComment(Long announcementId) {
        if (announcementId == null) {
            return 0L;
        }
        try {
            return commentMapper.selectCount(Wrappers.<Comment>lambdaQuery()
                    .eq(Comment::getAnnouncementId, announcementId));
        } catch (Exception e) {
            log.warn("评论数统计失败，announcementId={}，原因：{}", announcementId, e.getMessage());
            return 0L;
        }
    }

    private List<Long> listUserIdsByRole(String roleCode) {
        List<User> users = userMapper.selectList(Wrappers.<User>lambdaQuery()
                .eq(User::getRoleCode, roleCode)
                .eq(User::getStatus, 1));
        return users.stream().map(User::getId).collect(Collectors.toList());
    }

    private String resolveUserName(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        return user.getRealName() == null || user.getRealName().isBlank() ? user.getUsername() : user.getRealName();
    }

    private boolean canOperate(LoginUser current, Announcement announcement) {
        if (current == null) {
            return false;
        }
        if (ROLE_SUPER_ADMIN.equals(current.getRoleCode()) || ROLE_UNIV_AUDITOR.equals(current.getRoleCode())) {
            return true;
        }
        return current.getUserId() != null && current.getUserId().equals(announcement.getAuthorId());
    }

    private boolean visibleByScope(Announcement announcement, LoginUser current) {
        String visibility = announcement.getVisibility();
        if (visibility == null || VISIBILITY_ALL.equals(visibility)) {
            return true;
        }
        if (current == null || current.getDeptId() == null) {
            return false;
        }
        if (VISIBILITY_COLLEGE.equals(visibility) || VISIBILITY_CLASS.equals(visibility)) {
            if (current.getDeptId().equals(announcement.getDeptId())) {
                return true;
            }
            List<AnnouncementScope> scopes = scopeMapper.selectList(Wrappers.<AnnouncementScope>lambdaQuery()
                    .eq(AnnouncementScope::getAnnouncementId, announcement.getId())
                    .in(AnnouncementScope::getScopeType, VISIBILITY_COLLEGE, VISIBILITY_CLASS));
            for (AnnouncementScope scope : scopes) {
                if (current.getDeptId().equals(scope.getScopeValue())) {
                    return true;
                }
            }
            return false;
        }
        if (VISIBILITY_CUSTOM.equals(visibility)) {
            List<AnnouncementScope> scopes = scopeMapper.selectList(Wrappers.<AnnouncementScope>lambdaQuery()
                    .eq(AnnouncementScope::getAnnouncementId, announcement.getId()));
            for (AnnouncementScope scope : scopes) {
                if (scope.getScopeValue() == null) {
                    continue;
                }
                if ("USER".equals(scope.getScopeType()) && scope.getScopeValue().equals(current.getUserId())) {
                    return true;
                }
                if (!"USER".equals(scope.getScopeType()) && scope.getScopeValue().equals(current.getDeptId())) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private Announcement requireAnnouncement(Long id) {
        if (id == null) {
            throw new BizException("公告ID不能为空");
        }
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw new BizException("公告不存在");
        }
        return announcement;
    }

    private LoginUser requireLogin() {
        LoginUser loginUser = UserContext.get();
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new BizException("未登录或登录已过期");
        }
        return loginUser;
    }

    private void clearStatsCache() {
        try {
            Set<String> keys = stringRedisTemplate.keys(Constants.REDIS_STATS_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("清理统计缓存失败：{}", e.getMessage());
        }
    }

    private String joinText(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part != null) {
                sb.append(part).append('\n');
            }
        }
        return sb.toString();
    }

    private String buildSummary(String content) {
        if (content == null) {
            return null;
        }
        String plain = content.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim();
        return plain.length() <= 120 ? plain : plain.substring(0, 120);
    }

    private String safeText(String text) {
        return text == null || text.isBlank() ? "无" : text;
    }

    private String statusText(String status) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case STATUS_DRAFT:
                return "草稿";
            case STATUS_PENDING:
                return "待审核";
            case STATUS_PUBLISHED:
                return "已发布";
            case STATUS_SCHEDULED:
                return "定时发布";
            case STATUS_REJECTED:
                return "已驳回";
            case STATUS_WITHDRAWN:
                return "已撤回";
            case STATUS_ARCHIVED:
                return "已归档";
            default:
                return status;
        }
    }

    private String nodeText(String node) {
        if (node == null) {
            return "";
        }
        switch (node) {
            case NODE_SUBMIT:
                return "提交审核";
            case NODE_DEPT_AUDIT:
                return "院系审核";
            case NODE_UNIV_AUDIT:
                return "校级审核";
            case NODE_PUBLISH:
                return "发布";
            case NODE_REJECT:
                return "驳回";
            case NODE_WITHDRAW:
                return "撤回";
            case NODE_ARCHIVE:
                return "归档";
            case NODE_UPDATE:
                return "编辑";
            default:
                return node;
        }
    }
}
