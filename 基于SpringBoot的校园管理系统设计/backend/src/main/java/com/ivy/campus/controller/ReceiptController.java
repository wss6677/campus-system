package com.ivy.campus.controller;

import com.ivy.campus.common.R;
import com.ivy.campus.dto.UrgeDTO;
import com.ivy.campus.security.RequireRole;
import com.ivy.campus.service.ReceiptService;
import com.ivy.campus.vo.DeptUnreadVO;
import com.ivy.campus.vo.ReceiptVO;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/receipt")
@RequiredArgsConstructor
@Validated
@Tag(name = "回执管理", description = "已读回执、阅读统计、催办")
public class ReceiptController {

    private final ReceiptService receiptService;

    @PostMapping("/read/{announcementId}")
    @Operation(summary = "标记已读")
    public R<Void> read(@PathVariable Long announcementId) {
        receiptService.markRead(announcementId);
        return R.ok("已标记已读", null);
    }

    @GetMapping("/stat/{announcementId}")
    @RequireRole({"SUPER_ADMIN", "UNIV_AUDITOR", "DEPT_PUBLISHER"})
    @Operation(summary = "回执统计")
    public R<ReceiptVO> stat(@PathVariable Long announcementId) {
        return R.ok(receiptService.statByAnnouncement(announcementId));
    }

    @GetMapping("/unread/{announcementId}")
    @RequireRole({"SUPER_ADMIN", "UNIV_AUDITOR", "DEPT_PUBLISHER"})
    @Operation(summary = "部门未读排行")
    public R<List<DeptUnreadVO>> unread(@PathVariable Long announcementId) {
        return R.ok(receiptService.unreadRank(announcementId));
    }

    @PostMapping("/urge")
    @RequireRole({"SUPER_ADMIN", "UNIV_AUDITOR", "DEPT_PUBLISHER"})
    @Operation(summary = "催办未读用户")
    public R<Long> urge(@Valid @RequestBody UrgeDTO dto) {
        return R.ok("催办成功", receiptService.urge(dto.getAnnouncementId(), dto.getDeptId()));
    }
}
