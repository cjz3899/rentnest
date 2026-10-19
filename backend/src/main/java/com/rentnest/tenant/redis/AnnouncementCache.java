package com.rentnest.tenant.redis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentnest.common.page.PageResult;
import com.rentnest.tenant.dto.AnnouncementResp;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;

/**
 * 已发布公告列表缓存。公告读多写少，5 分钟短缓存 + 写时整片失效即可
 */
@Slf4j
@Component
public class AnnouncementCache {
    public static final String KEY_PREFIX = "rentnest:cache:ann:page:";
    private static final Duration TTL = Duration.ofMinutes(5);

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ObjectMapper objectMapper;

    public PageResult<AnnouncementResp> get(Object queryKey) {
        try {
            String json = stringRedisTemplate.opsForValue().get(key(queryKey));
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.warn("announcement cache read failed, fallback to db", e);
            return null;
        }
    }

    public void put(Object queryKey, PageResult<AnnouncementResp> result) {
        try {
            stringRedisTemplate.opsForValue().set(key(queryKey),
                    objectMapper.writeValueAsString(result), TTL);
        } catch (Exception e) {
            log.warn("announcement cache write failed", e);
        }
    }

    /**
     * 公告任何变更时整片失效
     */
    public void evictAll() {
        try {
            Set<String> keys = stringRedisTemplate.keys(KEY_PREFIX + "*");
            if (!keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("announcement cache evict failed", e);
        }
    }

    private String key(Object queryKey) {
        return KEY_PREFIX + Objects.hashCode(queryKey);
    }
}
