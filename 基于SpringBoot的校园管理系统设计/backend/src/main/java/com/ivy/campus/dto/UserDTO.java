package com.ivy.campus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "用户新增/修改入参")
public class UserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "用户ID，新增时为空")
    private Long id;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度需在3-50之间")
    @Schema(description = "用户名")
    private String username;

    @Size(min = 6, max = 64, message = "密码长度需在6-64之间")
    @Schema(description = "密码，修改时为空表示不修改")
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 50, message = "真实姓名长度不能超过50")
    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱")
    private String email;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "性别 0未知 1男 2女")
    private Integer gender;

    @Schema(description = "所属部门ID")
    private Long deptId;

    @Schema(description = "学号/工号")
    private String studentNo;

    @Schema(description = "用户类型 STUDENT/TEACHER/STAFF")
    private String userType;

    @Schema(description = "角色码 SUPER_ADMIN/UNIV_AUDITOR/DEPT_PUBLISHER/TEACHER/STUDENT")
    private String roleCode;

    @Schema(description = "状态 1正常0停用")
    private Integer status;
}
