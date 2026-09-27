package com.github.alonsopelaezflores.ratelimit.internal;

import com.github.alonsopelaezflores.ratelimit.store.RateLimitStore;

import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRateLimitStore implements RateLimitStore {
    private final ConcurrentHashMap<String, Bucket> buckets= new ConcurrentHashMap<>();

    @Override
    public boolean tryConsume(String key, int capacity, int refillTokens, long refillPeriodMillis) {
        Bucket bucket = buckets.computeIfAbsent(key,
                k -> new Bucket(capacity, refillTokens, refillPeriodMillis));
        return bucket.tryConsume();
    }
}
