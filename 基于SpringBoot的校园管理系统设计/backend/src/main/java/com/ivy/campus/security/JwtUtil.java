package com.ivy.campus.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 签发与解析
 */
@Slf4j
@Component
public class JwtUtil {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLE_CODE = "roleCode";

    @Value("${campus.jwt.secret}")
    private String secret;

    @Value("${campus.jwt.expire:86400}")
    private Long expire;

    /** 签发 token */
    public String generateToken(Long userId, String username, String roleCode) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims()
                .add(CLAIM_USER_ID, userId)
                .add(CLAIM_USERNAME, username)
                .add(CLAIM_ROLE_CODE, roleCode)
                .and()
                .subject(String.valueOf(userId))
                .issuedAt(new Date(now))
                .expiration(new Date(now + expire * 1000L))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();
    }

    /** 解析所有声明 */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** 取用户主键 */
    public Long getUserId(String token) {
        try {
            Object value = parse(token).get(CLAIM_USER_ID);
            return value == null ? null : Long.valueOf(value.toString());
        } catch (Exception e) {
            log.debug("解析 token 用户失败 message={}", e.getMessage());
            return null;
        }
    }

    /** 取登录名 */
    public String getUsername(String token) {
        try {
            Object value = parse(token).get(CLAIM_USERNAME);
            return value == null ? null : value.toString();
        } catch (Exception e) {
            log.debug("解析 token 用户名失败 message={}", e.getMessage());
            return null;
        }
    }

    /** 取角色码 */
    public String getRoleCode(String token) {
        try {
            Object value = parse(token).get(CLAIM_ROLE_CODE);
            return value == null ? null : value.toString();
        } catch (Exception e) {
            log.debug("解析 token 角色失败 message={}", e.getMessage());
            return null;
        }
    }

    /** 校验 token 是否有效 */
    public boolean validate(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Claims claims = parse(token);
            Date expiration = claims.getExpiration();
            return expiration == null || expiration.after(new Date());
        } catch (Exception e) {
            log.debug("token 校验失败 message={}", e.getMessage());
            return false;
        }
    }

    /** 有效期秒数 */
    public Long getExpire() {
        return expire;
    }
}
