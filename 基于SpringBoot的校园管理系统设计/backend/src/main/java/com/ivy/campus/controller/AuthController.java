package com.ivy.campus.controller;

import com.ivy.campus.common.R;
import com.ivy.campus.dto.LoginDTO;
import com.ivy.campus.dto.RegisterDTO;
import com.ivy.campus.service.AuthService;
import com.ivy.campus.vo.LoginVO;
import com.ivy.campus.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "认证管理", description = "登录、注册、验证码、当前用户")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "登录")
    public R<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return R.ok("登录成功", authService.login(dto));
    }

    @PostMapping("/register")
    @Operation(summary = "注册")
    public R<Void> register(@Valid @RequestBody RegisterDTO dto) {
        authService.register(dto);
        return R.ok("注册成功", null);
    }

    @GetMapping("/captcha")
    @Operation(summary = "获取验证码")
    public R<Map<String, String>> captcha() {
        return R.ok(authService.captcha());
    }

    @GetMapping("/info")
    @Operation(summary = "当前登录人信息")
    public R<UserVO> info() {
        return R.ok(authService.info());
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public R<Void> logout() {
        authService.logout();
        return R.ok("退出成功", null);
    }
}
