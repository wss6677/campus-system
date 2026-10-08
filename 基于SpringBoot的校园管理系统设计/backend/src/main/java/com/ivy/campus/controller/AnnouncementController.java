package com.ivy.campus.controller;

import com.ivy.campus.common.PageResult;
import com.ivy.campus.common.R;
import com.ivy.campus.dto.AnnouncementDTO;
import com.ivy.campus.dto.AnnouncementQuery;
import com.ivy.campus.security.RequireRole;
import com.ivy.campus.service.AnnouncementService;
import com.ivy.campus.vo.AnnouncementDetailVO;
import com.ivy.campus.vo.AnnouncementVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/announcement")
@RequiredArgsConstructor
@Validated
@Tag(name = "公告管理", description = "公告的查询、保存、提交、发布、撤回、归档")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping("/page")
    @Operation(summary = "公告分页")
    public R<PageResult<AnnouncementVO>> page(@Valid AnnouncementQuery query) {
        return R.ok(announcementService.page(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "公告详情")
    public R<AnnouncementDetailVO> detail(@PathVariable Long id,
                                          @RequestParam(defaultValue = "true") boolean countView) {
        return R.ok(announcementService.detail(id, countView));
    }

    @PostMapping("/save")
    @RequireRole({"DEPT_PUBLISHER", "UNIV_AUDITOR", "SUPER_ADMIN"})
    @Operation(summary = "保存公告草稿")
    public R<Long> save(@Valid @RequestBody AnnouncementDTO dto) {
        return R.ok("保存成功", announcementService.saveDraft(dto));
    }

    @PutMapping("/{id}")
    @RequireRole({"DEPT_PUBLISHER", "UNIV_AUDITOR", "SUPER_ADMIN"})
    @Operation(summary = "修改公告")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody AnnouncementDTO dto) {
        announcementService.update(id, dto);
        return R.ok("修改成功", null);
    }

    @PostMapping("/{id}/submit")
    @RequireRole({"DEPT_PUBLISHER", "UNIV_AUDITOR", "SUPER_ADMIN"})
    @Operation(summary = "提交审核")
    public R<Void> submit(@PathVariable Long id) {
        announcementService.submit(id);
        return R.ok("提交成功", null);
    }

    @PostMapping("/{id}/publish")
    @RequireRole({"DEPT_PUBLISHER", "UNIV_AUDITOR", "SUPER_ADMIN"})
    @Operation(summary = "直接发布")
    public R<Void> publish(@PathVariable Long id) {
        announcementService.publish(id);
        return R.ok("发布成功", null);
    }

    @PostMapping("/{id}/withdraw")
    @RequireRole({"DEPT_PUBLISHER", "UNIV_AUDITOR", "SUPER_ADMIN"})
    @Operation(summary = "撤回公告")
    public R<Void> withdraw(@PathVariable Long id) {
        announcementService.withdraw(id);
        return R.ok("撤回成功", null);
    }

    @PostMapping("/{id}/archive")
    @RequireRole({"DEPT_PUBLISHER", "UNIV_AUDITOR", "SUPER_ADMIN"})
    @Operation(summary = "归档公告")
    public R<Void> archive(@PathVariable Long id) {
        announcementService.archive(id);
        return R.ok("归档成功", null);
    }

    @PostMapping("/{id}/top")
    @RequireRole({"DEPT_PUBLISHER", "UNIV_AUDITOR", "SUPER_ADMIN"})
    @Operation(summary = "置顶/取消置顶")
    public R<Void> toggleTop(@PathVariable Long id) {
        announcementService.toggleTop(id);
        return R.ok("操作成功", null);
    }

    @PostMapping("/{id}/banner")
    @RequireRole({"DEPT_PUBLISHER", "UNIV_AUDITOR", "SUPER_ADMIN"})
    @Operation(summary = "轮播/取消轮播")
    public R<Void> toggleBanner(@PathVariable Long id) {
        announcementService.toggleBanner(id);
        return R.ok("操作成功", null);
    }

    @DeleteMapping("/{id}")
    @RequireRole({"DEPT_PUBLISHER", "UNIV_AUDITOR", "SUPER_ADMIN"})
    @Operation(summary = "删除公告")
    public R<Void> remove(@PathVariable Long id) {
        announcementService.remove(id);
        return R.ok("删除成功", null);
    }

    @GetMapping("/latest")
    @Operation(summary = "最新公告")
    public R<List<AnnouncementVO>> latest(@RequestParam(defaultValue = "10") Integer limit) {
        return R.ok(announcementService.latest(limit));
    }

    @GetMapping("/banner")
    @Operation(summary = "轮播公告")
    public R<List<AnnouncementVO>> banner() {
        return R.ok(announcementService.banner());
    }
}
