package com.ivy.campus.controller;

import com.ivy.campus.common.PageResult;
import com.ivy.campus.common.R;
import com.ivy.campus.dto.DeptDTO;
import com.ivy.campus.security.RequireRole;
import com.ivy.campus.service.DeptService;
import com.ivy.campus.vo.DeptVO;
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
@RequestMapping("/api/dept")
@RequiredArgsConstructor
@Validated
@Tag(name = "组织架构", description = "部门树与部门维护")
public class DeptController {

    private final DeptService deptService;

    @GetMapping("/tree")
    @Operation(summary = "部门树")
    public R<List<DeptVO>> tree() {
        return R.ok(deptService.tree());
    }

    @GetMapping("/list")
    @Operation(summary = "部门平铺列表")
    public R<List<DeptVO>> list() {
        return R.ok(deptService.listAll());
    }

    @GetMapping("/page")
    @Operation(summary = "部门分页")
    public R<PageResult<DeptVO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                      @RequestParam(defaultValue = "10") Integer pageSize,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) String type,
                                      @RequestParam(required = false) Long parentId) {
        return R.ok(deptService.page(pageNum, pageSize, keyword, type, parentId));
    }

    @PostMapping
    @RequireRole({"SUPER_ADMIN"})
    @Operation(summary = "新增部门")
    public R<Long> save(@Valid @RequestBody DeptDTO dto) {
        return R.ok("新增成功", deptService.save(dto));
    }

    @PutMapping("/{id}")
    @RequireRole({"SUPER_ADMIN"})
    @Operation(summary = "修改部门")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody DeptDTO dto) {
        deptService.update(id, dto);
        return R.ok("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @RequireRole({"SUPER_ADMIN"})
    @Operation(summary = "删除部门")
    public R<Void> remove(@PathVariable Long id) {
        deptService.remove(id);
        return R.ok("删除成功", null);
    }
}
