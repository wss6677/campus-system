package com.ivy.campus.controller;

import com.ivy.campus.common.PageResult;
import com.ivy.campus.common.R;
import com.ivy.campus.dto.CategoryDTO;
import com.ivy.campus.entity.Category;
import com.ivy.campus.security.RequireRole;
import com.ivy.campus.service.CategoryService;
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
@RequestMapping("/api/category")
@RequiredArgsConstructor
@Validated
@Tag(name = "分类管理", description = "公告分类维护")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/list")
    @Operation(summary = "分类下拉列表")
    public R<List<Category>> list() {
        return R.ok(categoryService.listAll());
    }

    @GetMapping("/page")
    @Operation(summary = "分类分页")
    public R<PageResult<Category>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) Integer status) {
        return R.ok(categoryService.page(pageNum, pageSize, keyword, status));
    }

    @PostMapping
    @RequireRole({"SUPER_ADMIN"})
    @Operation(summary = "新增分类")
    public R<Long> save(@Valid @RequestBody CategoryDTO dto) {
        return R.ok("新增成功", categoryService.save(dto));
    }

    @PutMapping("/{id}")
    @RequireRole({"SUPER_ADMIN"})
    @Operation(summary = "修改分类")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody CategoryDTO dto) {
        categoryService.update(id, dto);
        return R.ok("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @RequireRole({"SUPER_ADMIN"})
    @Operation(summary = "删除分类")
    public R<Void> remove(@PathVariable Long id) {
        categoryService.remove(id);
        return R.ok("删除成功", null);
    }
}
