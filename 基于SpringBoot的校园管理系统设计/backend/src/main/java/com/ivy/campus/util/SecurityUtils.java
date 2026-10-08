package com.ivy.campus.util;

import com.ivy.campus.common.Constants;
import com.ivy.campus.security.LoginUser;
import com.ivy.campus.security.UserContext;

import java.util.Arrays;

/**
 * 登录用户便捷访问工具
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static LoginUser getUser() {
        return UserContext.get();
    }

    public static Long userId() {
        return UserContext.getUserId();
    }

    public static String username() {
        LoginUser loginUser = UserContext.get();
        return loginUser == null ? null : loginUser.getUsername();
    }

    public static String realName() {
        LoginUser loginUser = UserContext.get();
        return loginUser == null ? null : loginUser.getRealName();
    }

    public static String roleCode() {
        return UserContext.getRoleCode();
    }

    public static Long deptId() {
        return UserContext.getDeptId();
    }

    public static boolean hasRole(String... roles) {
        if (roles == null || roles.length == 0) {
            return true;
        }
        String roleCode = roleCode();
        return roleCode != null && Arrays.asList(roles).contains(roleCode);
    }

    public static boolean isAdmin() {
        return Constants.ROLE_SUPER_ADMIN.equals(roleCode());
    }

    public static boolean isAuditor() {
        return hasRole(Constants.ROLE_UNIV_AUDITOR, Constants.ROLE_SUPER_ADMIN);
    }

    public static boolean isPublisher() {
        return hasRole(Constants.ROLE_DEPT_PUBLISHER, Constants.ROLE_UNIV_AUDITOR, Constants.ROLE_SUPER_ADMIN);
    }
}
