package com.rentnest.rental.redis;

import java.time.Duration;

public class CacheConstant {
    public static final String KEY_PREFIX = "rentnest:cache:house:page:";
    public static final Duration TTL = Duration.ofMinutes(10);
}
