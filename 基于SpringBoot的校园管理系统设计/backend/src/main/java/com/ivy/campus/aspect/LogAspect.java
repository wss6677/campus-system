package com.ivy.campus.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ivy.campus.entity.OperationLog;
import com.ivy.campus.security.LoginUser;
import com.ivy.campus.security.UserContext;
import com.ivy.campus.service.LogService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class LogAspect {

    private static final int MAX_PARAM_LENGTH = 2000;

    private final LogService logService;

    private final ObjectMapper objectMapper;

    @Pointcut("execution(* com.ivy.campus.controller..*(..))")
    public void controllerPointcut() {
    }

    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        OperationLog operationLog = new OperationLog();
        Throwable error = null;
        try {
            Object result = joinPoint.proceed();
            return result;
        } catch (Throwable throwable) {
            error = throwable;
            throw throwable;
        } finally {
            try {
                fillLog(operationLog, joinPoint, System.currentTimeMillis() - start, error);
                logService.asyncSave(operationLog);
            } catch (Exception e) {
                log.warn("操作日志采集失败：{}", e.getMessage());
            }
        }
    }

    private void fillLog(OperationLog operationLog, ProceedingJoinPoint joinPoint, long costTime, Throwable error) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        operationLog.setMethod(signature.getDeclaringTypeName() + "." + signature.getName());
        operationLog.setModule(resolveModule(signature.getDeclaringTypeName()));
        operationLog.setOperation(resolveOperation(method, signature.getName()));
        operationLog.setParams(resolveParams(joinPoint.getArgs()));
        operationLog.setCostTime(costTime);
        operationLog.setStatus(error == null ? 1 : 0);
        if (error != null) {
            String message = error.getMessage();
            operationLog.setErrorMsg(message == null ? error.getClass().getSimpleName() : message);
        }
        LoginUser loginUser = UserContext.get();
        if (loginUser != null) {
            operationLog.setUserId(loginUser.getUserId());
            operationLog.setUsername(loginUser.getUsername());
        }
        HttpServletRequest request = currentRequest();
        if (request != null) {
            operationLog.setIp(resolveIp(request));
            operationLog.setLocation(request.getRequestURI());
        }
        operationLog.setCreateTime(LocalDateTime.now());
    }

    private String resolveModule(String className) {
        int index = className.lastIndexOf('.');
        String simpleName = index > -1 ? className.substring(index + 1) : className;
        return simpleName.endsWith("Controller")
                ? simpleName.substring(0, simpleName.length() - "Controller".length())
                : simpleName;
    }

    private String resolveOperation(Method method, String methodName) {
        Operation annotation = method.getAnnotation(Operation.class);
        if (annotation != null && annotation.summary() != null && !annotation.summary().isBlank()) {
            return annotation.summary();
        }
        return methodName;
    }

    private String resolveParams(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        List<Object> serializable = new ArrayList<>();
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            if (arg instanceof MultipartFile || arg instanceof jakarta.servlet.http.HttpServletResponse
                    || arg instanceof jakarta.servlet.http.HttpServletRequest) {
                serializable.add(arg.getClass().getSimpleName());
                continue;
            }
            serializable.add(arg);
        }
        try {
            String json = objectMapper.writeValueAsString(serializable);
            return json.length() > MAX_PARAM_LENGTH ? json.substring(0, MAX_PARAM_LENGTH) : json;
        } catch (Exception e) {
            return "参数序列化失败：" + e.getClass().getSimpleName();
        }
    }

    private HttpServletRequest currentRequest() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes == null ? null : attributes.getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
