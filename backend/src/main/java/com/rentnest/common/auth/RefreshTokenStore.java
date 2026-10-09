package com.rentnest.common.auth;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RefreshTokenStore {
    private static final String PREFIX = "auth:refresh:";

    private final StringRedisTemplate redis;

    public RefreshTokenStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void save(Long userId, String jti, Duration ttl) {
        String key = key(userId);
        redis.opsForHash().put(key, jti, String.valueOf(System.currentTimeMillis() + ttl.toMillis()));
        redis.expire(key, ttl);
    }

    public boolean exists(Long userId, String jti) {
        return redis.opsForHash().hasKey(key(userId), jti);
    }

    public void revoke(Long userId, String jti) {
        redis.opsForHash().delete(key(userId), jti);
    }

    public void revokeAll(Long userId) {
        redis.delete(key(userId));
    }

    private String key(Long userId) {
        return PREFIX + userId;
    }
}
