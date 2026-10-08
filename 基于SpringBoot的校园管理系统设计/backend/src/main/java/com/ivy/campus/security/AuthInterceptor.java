package com.ivy.campus.security;

import cn.hutool.core.convert.Convert;
import com.ivy.campus.common.Constants;
import com.ivy.campus.common.R;
import com.ivy.campus.common.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 登录态与角色拦截器，拦截 /api/**
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = resolveToken(request);
        if (token == null || !jwtUtil.validate(token)) {
            writeError(response, ResultCode.UNAUTHORIZED);
            return false;
        }

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(jwtUtil.getUserId(token));
        loginUser.setUsername(jwtUtil.getUsername(token));
        loginUser.setRoleCode(jwtUtil.getRoleCode(token));
        loginUser.setRealName(request.getHeader("X-Real-Name"));
        loginUser.setDeptId(Convert.toLong(request.getHeader("X-Dept-Id"), null));
        loginUser.setDataScope(request.getHeader("X-Data-Scope"));
        loginUser.setUserType(Convert.toInt(request.getHeader("X-User-Type"), null));
        UserContext.set(loginUser);

        if (handler instanceof HandlerMethod handlerMethod) {
            RequireRole requireRole = resolveRequireRole(handlerMethod);
            if (requireRole != null && requireRole.value().length > 0) {
                String roleCode = loginUser.getRoleCode();
                Set<String> allowed = Arrays.stream(requireRole.value()).collect(Collectors.toSet());
                if (roleCode == null || !allowed.contains(roleCode)) {
                    log.warn("角色校验未通过 uri={} roleCode={} allowed={}", request.getRequestURI(), roleCode, allowed);
                    UserContext.clear();
                    writeError(response, ResultCode.FORBIDDEN);
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    /** 从请求头或 token 参数中提取令牌 */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(Constants.TOKEN_HEADER);
        if (header != null && !header.isBlank()) {
            return header.startsWith(Constants.TOKEN_PREFIX) ? header.substring(Constants.TOKEN_PREFIX.length()).trim() : header.trim();
        }
        String token = request.getParameter("token");
        return (token == null || token.isBlank()) ? null : token.trim();
    }

    /** 方法级注解优先于类级注解 */
    private RequireRole resolveRequireRole(HandlerMethod handlerMethod) {
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
        }
        return requireRole;
    }

    /** 输出统一未授权 / 无权限响应 */
    private void writeError(HttpServletResponse response, ResultCode resultCode) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        R<Void> body = R.fail(resultCode.getCode(), resultCode.getMessage());
        response.getWriter().write(objectMapper.writeValueAsString(body));
        response.getWriter().flush();
    }
}
