package com.ivy.campus.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端真实 IP 工具
 */
public final class IpUtils {

    private static final String UNKNOWN = "unknown";

    private static final String[] HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    };

    private IpUtils() {
    }

    /** 获取客户端真实 IP，兼容多级代理 */
    public static String getIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }
        for (String header : HEADERS) {
            String value = request.getHeader(header);
            if (isValid(value)) {
                int index = value.indexOf(',');
                String ip = index > 0 ? value.substring(0, index) : value;
                return normalize(ip.trim());
            }
        }
        String remoteAddr = request.getRemoteAddr();
        return normalize(remoteAddr);
    }

    /** 判断是否为内网地址 */
    public static boolean isInternalIp(String ip) {
        if (ip == null || ip.isBlank() || UNKNOWN.equalsIgnoreCase(ip)) {
            return true;
        }
        return "127.0.0.1".equals(ip)
                || "0:0:0:0:0:0:0:1".equals(ip)
                || "::1".equals(ip)
                || "localhost".equalsIgnoreCase(ip)
                || ip.startsWith("10.")
                || ip.startsWith("192.168.")
                || isPrivate172(ip);
    }

    private static boolean isPrivate172(String ip) {
        if (!ip.startsWith("172.")) {
            return false;
        }
        String[] parts = ip.split("\\.");
        if (parts.length < 2) {
            return false;
        }
        try {
            int second = Integer.parseInt(parts[1]);
            return second >= 16 && second <= 31;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static String normalize(String ip) {
        if (ip == null || ip.isBlank()) {
            return UNKNOWN;
        }
        return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
    }

    private static boolean isValid(String value) {
        return value != null && !value.isBlank() && !UNKNOWN.equalsIgnoreCase(value.trim());
    }
}
