package com.ivy.campus.controller;

import com.ivy.campus.common.PageResult;
import com.ivy.campus.common.R;
import com.ivy.campus.dto.TemplateDTO;
import com.ivy.campus.entity.Template;
import com.ivy.campus.security.RequireRole;
import com.ivy.campus.service.TemplateService;
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
@RequestMapping("/api/template")
@RequiredArgsConstructor
@Validated
@Tag(name = "模板管理", description = "公告模板维护")
public class TemplateController {

    private final TemplateService templateService;

    @GetMapping("/page")
    @Operation(summary = "模板分页")
    public R<PageResult<Template>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) Long categoryId) {
        return R.ok(templateService.page(pageNum, pageSize, keyword, categoryId));
    }

    @GetMapping("/list")
    @Operation(summary = "模板下拉列表")
    public R<List<Template>> list() {
        return R.ok(templateService.listAll());
    }

    @PostMapping
    @RequireRole({"SUPER_ADMIN", "UNIV_AUDITOR", "DEPT_PUBLISHER"})
    @Operation(summary = "新增模板")
    public R<Long> save(@Valid @RequestBody TemplateDTO dto) {
        return R.ok("新增成功", templateService.save(dto));
    }

    @PutMapping("/{id}")
    @RequireRole({"SUPER_ADMIN", "UNIV_AUDITOR", "DEPT_PUBLISHER"})
    @Operation(summary = "修改模板")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody TemplateDTO dto) {
        templateService.update(id, dto);
        return R.ok("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @RequireRole({"SUPER_ADMIN", "UNIV_AUDITOR", "DEPT_PUBLISHER"})
    @Operation(summary = "删除模板")
    public R<Void> remove(@PathVariable Long id) {
        templateService.remove(id);
        return R.ok("删除成功", null);
    }
}
