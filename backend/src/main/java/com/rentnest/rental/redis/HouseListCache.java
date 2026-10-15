package com.rentnest.rental.redis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentnest.common.page.PageResult;
import com.rentnest.rental.dto.HouseListItemResp;
import com.rentnest.rental.dto.HouseQuery;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

import static com.rentnest.rental.redis.CacheConstant.KEY_PREFIX;
import static com.rentnest.rental.redis.CacheConstant.TTL;


@Slf4j
@Component
public class HouseListCache {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ObjectMapper objectMapper;

    public PageResult<HouseListItemResp> get(HouseQuery query) {
        try {
            String json = stringRedisTemplate.opsForValue().get(key(query));
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.warn("house cache read failed, fallback to db", e);
            return null;
        }
    }

    public void put(HouseQuery query, PageResult<HouseListItemResp> result) {
        try {
            stringRedisTemplate.opsForValue().set(key(query),
                    objectMapper.writeValueAsString(result), TTL);
        } catch (Exception e) {
            log.warn("house cache write failed", e);
        }
    }

    /**
     * 房源任何变更时整片失效（筛选组合多，精确失效不划算）
     */
    public void evictAll() {
        try {
            Set<String> keys = stringRedisTemplate.keys(KEY_PREFIX + "*");
            if (!keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("house cache evict failed", e);
        }
    }

    private String key(HouseQuery query) {
        return KEY_PREFIX + Objects.hashCode(query);
    }
}
