package com.ivy.campus.controller;

import com.ivy.campus.common.PageResult;
import com.ivy.campus.common.R;
import com.ivy.campus.dto.CommentDTO;
import com.ivy.campus.security.RequireRole;
import com.ivy.campus.service.CommentService;
import com.ivy.campus.vo.CommentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/comment")
@RequiredArgsConstructor
@Validated
@Tag(name = "评论管理", description = "公告评论的发布、审核、删除")
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/page")
    @Operation(summary = "公告评论分页")
    public R<PageResult<CommentVO>> page(@RequestParam Long announcementId,
                                         @RequestParam(defaultValue = "1") Integer pageNum,
                                         @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(commentService.pageByAnnouncement(announcementId, pageNum, pageSize));
    }

    @PostMapping
    @Operation(summary = "发表评论")
    public R<Long> add(@Valid @RequestBody CommentDTO dto) {
        return R.ok("评论成功", commentService.add(dto));
    }

    @PostMapping("/{id}/audit")
    @RequireRole({"SUPER_ADMIN", "UNIV_AUDITOR"})
    @Operation(summary = "评论审核")
    public R<Void> audit(@PathVariable Long id, @RequestParam String status) {
        commentService.audit(id, status);
        return R.ok("审核完成", null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除评论")
    public R<Void> remove(@PathVariable Long id) {
        commentService.remove(id);
        return R.ok("删除成功", null);
    }
}
