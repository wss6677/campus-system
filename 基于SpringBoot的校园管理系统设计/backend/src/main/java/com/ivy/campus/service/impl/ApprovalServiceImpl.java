package com.ivy.campus.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ivy.campus.common.PageResult;
import com.ivy.campus.entity.Announcement;
import com.ivy.campus.entity.ApprovalLog;
import com.ivy.campus.entity.Category;
import com.ivy.campus.entity.Dept;
import com.ivy.campus.entity.User;
import com.ivy.campus.mapper.AnnouncementMapper;
import com.ivy.campus.mapper.ApprovalLogMapper;
import com.ivy.campus.mapper.CategoryMapper;
import com.ivy.campus.mapper.DeptMapper;
import com.ivy.campus.mapper.UserMapper;
import com.ivy.campus.service.ApprovalService;
import com.ivy.campus.util.PageUtils;
import com.ivy.campus.vo.AnnouncementVO;
import com.ivy.campus.vo.ApprovalLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalServiceImpl implements ApprovalService {

    private static final String STATUS_PENDING = "PENDING";

    private static final String NODE_SUBMIT = "SUBMIT";
    private static final String NODE_DEPT_AUDIT = "DEPT_AUDIT";
    private static final String NODE_UNIV_AUDIT = "UNIV_AUDIT";
    private static final String NODE_PUBLISH = "PUBLISH";
    private static final String NODE_REJECT = "REJECT";
    private static final String NODE_WITHDRAW = "WITHDRAW";
    private static final String NODE_ARCHIVE = "ARCHIVE";
    private static final String NODE_UPDATE = "UPDATE";

    private final ApprovalLogMapper approvalLogMapper;
    private final AnnouncementMapper announcementMapper;
    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;
    private final DeptMapper deptMapper;

    @Override
    public List<ApprovalLogVO> timeline(Long announcementId) {
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

    @Override
    public PageResult<AnnouncementVO> todoList(Integer pageNum, Integer pageSize) {
        IPage<Announcement> page = announcementMapper.selectPage(PageUtils.of(pageNum, pageSize),
                Wrappers.<Announcement>lambdaQuery()
                        .eq(Announcement::getStatus, STATUS_PENDING)
                        .orderByAsc(Announcement::getUpdateTime)
                        .orderByAsc(Announcement::getId));
        List<AnnouncementVO> records = new ArrayList<>();
        for (Announcement item : page.getRecords()) {
            AnnouncementVO vo = new AnnouncementVO();
            BeanUtils.copyProperties(item, vo);
            vo.setStatusText("待审核");
            if (item.getCategoryId() != null) {
                Category category = categoryMapper.selectById(item.getCategoryId());
                if (category != null) {
                    vo.setCategoryName(category.getName());
                    vo.setCategoryColor(category.getColor());
                }
            }
            if (item.getAuthorId() != null) {
                User author = userMapper.selectById(item.getAuthorId());
                if (author != null) {
                    vo.setAuthorName(author.getRealName() == null || author.getRealName().isBlank()
                            ? author.getUsername() : author.getRealName());
                }
            }
            if (item.getDeptId() != null) {
                Dept dept = deptMapper.selectById(item.getDeptId());
                if (dept != null) {
                    vo.setDeptName(dept.getName());
                }
            }
            if (vo.getViewCount() == null) {
                vo.setViewCount(0);
            }
            if (vo.getReceiptCount() == null) {
                vo.setReceiptCount(0);
            }
            if (vo.getTargetCount() == null) {
                vo.setTargetCount(0);
            }
            records.add(vo);
        }
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
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
