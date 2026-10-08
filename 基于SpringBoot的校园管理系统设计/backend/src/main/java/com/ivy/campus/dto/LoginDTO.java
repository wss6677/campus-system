package com.ivy.campus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "登录入参")
public class LoginDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过50")
    @Schema(description = "用户名/学工号")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 4, max = 64, message = "密码长度需在4-64之间")
    @Schema(description = "密码")
    private String password;

    @Schema(description = "验证码uuid（演示环境可为空）")
    private String captchaUuid;

    @Schema(description = "验证码（演示环境可为空）")
    private String captchaCode;
}
