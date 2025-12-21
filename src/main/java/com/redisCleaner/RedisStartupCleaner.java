package com.redisCleaner;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class RedisStartupCleaner {

    private final StringRedisTemplate redisTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void clearAppCacheOnStartup() {
        Set<String> keys = redisTemplate.keys("*");

        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
