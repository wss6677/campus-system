package com.ivy.campus.controller;

import com.ivy.campus.common.PageResult;
import com.ivy.campus.common.R;
import com.ivy.campus.dto.AuditDTO;
import com.ivy.campus.security.RequireRole;
import com.ivy.campus.service.AnnouncementService;
import com.ivy.campus.service.ApprovalService;
import com.ivy.campus.vo.AnnouncementVO;
import com.ivy.campus.vo.ApprovalLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/approval")
@RequiredArgsConstructor
@Validated
@RequireRole({"UNIV_AUDITOR", "SUPER_ADMIN"})
@Tag(name = "审核管理", description = "公告审核、审批轨迹、待办列表")
public class ApprovalController {

    private final ApprovalService approvalService;

    private final AnnouncementService announcementService;

    @PostMapping("/{id}/audit")
    @Operation(summary = "审核公告")
    public R<Void> audit(@PathVariable Long id, @Valid @RequestBody AuditDTO dto) {
        announcementService.audit(id, dto);
        return R.ok("审核完成", null);
    }

    @GetMapping("/timeline/{id}")
    @Operation(summary = "审批轨迹")
    public R<List<ApprovalLogVO>> timeline(@PathVariable Long id) {
        return R.ok(approvalService.timeline(id));
    }

    @GetMapping("/todo")
    @Operation(summary = "待审核列表")
    public R<PageResult<AnnouncementVO>> todo(@RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(approvalService.todoList(pageNum, pageSize));
    }
}
