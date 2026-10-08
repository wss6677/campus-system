package com.ivy.campus.controller;

import com.ivy.campus.common.PageResult;
import com.ivy.campus.common.R;
import com.ivy.campus.dto.ResetPwdDTO;
import com.ivy.campus.dto.UserDTO;
import com.ivy.campus.security.RequireRole;
import com.ivy.campus.service.UserService;
import com.ivy.campus.vo.UserVO;
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

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Validated
@RequireRole({"SUPER_ADMIN"})
@Tag(name = "用户管理", description = "用户维护与密码重置")
public class UserController {

    private final UserService userService;

    @GetMapping("/page")
    @Operation(summary = "用户分页")
    public R<PageResult<UserVO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                      @RequestParam(defaultValue = "10") Integer pageSize,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) String roleCode,
                                      @RequestParam(required = false) Long deptId,
                                      @RequestParam(required = false) Integer status) {
        return R.ok(userService.page(pageNum, pageSize, keyword, roleCode, deptId, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "用户详情")
    public R<UserVO> detail(@PathVariable Long id) {
        return R.ok(userService.getById(id));
    }

    @PostMapping
    @Operation(summary = "新增用户")
    public R<Long> save(@Valid @RequestBody UserDTO dto) {
        return R.ok("新增成功", userService.save(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改用户")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody UserDTO dto) {
        userService.update(id, dto);
        return R.ok("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户")
    public R<Void> remove(@PathVariable Long id) {
        userService.remove(id);
        return R.ok("删除成功", null);
    }

    @PostMapping("/resetPwd")
    @Operation(summary = "重置密码")
    public R<Void> resetPwd(@Valid @RequestBody ResetPwdDTO dto) {
        userService.resetPwd(dto.getUserId(), dto.getPassword());
        return R.ok("重置成功", null);
    }
}
