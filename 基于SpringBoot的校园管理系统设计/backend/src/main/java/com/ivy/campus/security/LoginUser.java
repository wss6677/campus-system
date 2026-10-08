package com.ivy.campus.security;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 登录用户上下文对象
 */
@Data
public class LoginUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;

    private String username;

    private String realName;

    private String roleCode;

    private Long deptId;

    private String dataScope;

    private Integer userType;
}
