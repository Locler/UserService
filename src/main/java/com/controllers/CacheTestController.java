package com.controllers;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cache-test")
public class CacheTestController {

    private final CacheManager cacheManager;

    public CacheTestController(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @GetMapping("/{cacheName}/{key}")
    public Object getFromCache(@PathVariable String cacheName, @PathVariable String key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) return "Cache not found";
        return cache.get(key, Object.class);
    }

    @DeleteMapping("/{cacheName}")
    public String clearCache(@PathVariable String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) return "Cache not found";
        cache.clear();
        return "Cache cleared: " + cacheName;
    }
}
