package com.ivy.campus.security;

/**
 * 基于 ThreadLocal 的登录用户上下文
 */
public final class UserContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUser loginUser) {
        HOLDER.set(loginUser);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        LoginUser loginUser = HOLDER.get();
        return loginUser == null ? null : loginUser.getUserId();
    }

    public static String getRoleCode() {
        LoginUser loginUser = HOLDER.get();
        return loginUser == null ? null : loginUser.getRoleCode();
    }

    public static Long getDeptId() {
        LoginUser loginUser = HOLDER.get();
        return loginUser == null ? null : loginUser.getDeptId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
