package com.rentnest.common.ratelimit;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class RateLimiter {

    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT = loadScript(
            "lua/ratelimit.lua");

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 固定窗口计数（Lua 保证原子性），Redis 不可用时放行——限流组件故障不应阻断业务
     */
    public boolean tryAcquire(String bizKey, int limit, Duration window) {
        String key = "rentnest:rate-limit:" + bizKey;
        try {
            Long allowed = stringRedisTemplate.execute(
                    RATE_LIMIT_SCRIPT,
                    List.of(key),
                    String.valueOf(window.getSeconds()),
                    String.valueOf(limit)
            );
            return allowed == 1L;
        } catch (Exception e) {
            log.warn("rate limiter unavailable, allow request. key={}", key, e);
            return true;
        }
    }

    private static DefaultRedisScript<Long> loadScript(String path) {
        try {
            String script = new String(
                    new ClassPathResource(path).getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );
            return new DefaultRedisScript<>(script, Long.class);
        } catch (IOException e) {
            throw new UncheckedIOException("加载限流脚本失败: " + path, e);
        }
    }
}
