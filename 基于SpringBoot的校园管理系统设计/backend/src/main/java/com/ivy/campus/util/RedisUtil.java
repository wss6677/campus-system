package com.ivy.campus.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * 通用 Redis 操作工具
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    /** 写入缓存，不设置过期时间 */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /** 写入缓存并设置过期秒数 */
    public void set(String key, Object value, long timeout) {
        if (timeout > 0) {
            redisTemplate.opsForValue().set(key, value, timeout, TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForValue().set(key, value);
        }
    }

    /** 读取缓存 */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        return value == null ? null : (T) value;
    }

    /** 读取缓存并转换为指定类型 */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        return clazz.isInstance(value) ? (T) value : null;
    }

    /** 删除缓存 */
    public boolean del(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    /** 批量删除缓存 */
    public long del(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }
        Long count = redisTemplate.delete(keys);
        return count == null ? 0L : count;
    }

    /** 按前缀删除缓存 */
    public long delByPrefix(String prefix) {
        Collection<String> keys = redisTemplate.keys(prefix + "*");
        if (keys == null || keys.isEmpty()) {
            return 0L;
        }
        Long count = redisTemplate.delete(keys);
        log.info("按前缀清理缓存 prefix={} count={}", prefix, count);
        return count == null ? 0L : count;
    }

    /** 自增计数 */
    public long incr(String key, long delta) {
        Long value = redisTemplate.opsForValue().increment(key, delta);
        return value == null ? 0L : value;
    }

    /** 自增计数，键不存在时设置过期时间 */
    public long incr(String key, long delta, long timeout) {
        Long value = redisTemplate.opsForValue().increment(key, delta);
        if (value != null && value == delta && timeout > 0) {
            expire(key, timeout);
        }
        return value == null ? 0L : value;
    }

    /** 自减计数 */
    public long decr(String key, long delta) {
        Long value = redisTemplate.opsForValue().decrement(key, delta);
        return value == null ? 0L : value;
    }

    /** 设置过期时间 */
    public boolean expire(String key, long timeout) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, TimeUnit.SECONDS));
    }

    /** 读取剩余过期秒数，-2 表示键不存在 */
    public long getExpire(String key) {
        Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return expire == null ? -2L : expire;
    }

    /** 判断键是否存在 */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
