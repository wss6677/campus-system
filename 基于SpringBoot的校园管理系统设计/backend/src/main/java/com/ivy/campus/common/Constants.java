package com.ivy.campus.common;

/**
 * 全局常量
 */
public final class Constants {

    /** 认证请求头 */
    public static final String TOKEN_HEADER = "Authorization";

    /** 认证前缀 */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** 登录态 Redis 前缀 */
    public static final String REDIS_TOKEN_PREFIX = "campus:token:";

    /** 统计缓存 Redis 前缀 */
    public static final String REDIS_STATS_PREFIX = "campus:stats:";

    /** 公告状态 */
    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_SCHEDULED = "SCHEDULED";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_WITHDRAWN = "WITHDRAWN";
    public static final String STATUS_ARCHIVED = "ARCHIVED";

    /** 角色码 */
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ROLE_UNIV_AUDITOR = "UNIV_AUDITOR";
    public static final String ROLE_DEPT_PUBLISHER = "DEPT_PUBLISHER";
    public static final String ROLE_TEACHER = "TEACHER";
    public static final String ROLE_STUDENT = "STUDENT";

    /** 默认密码（123456） */
    public static final String DEFAULT_PASSWORD = "123456";

    /** 逻辑删除标记 */
    public static final Integer NOT_DELETED = 0;
    public static final Integer DELETED = 1;

    private Constants() {
    }
}
