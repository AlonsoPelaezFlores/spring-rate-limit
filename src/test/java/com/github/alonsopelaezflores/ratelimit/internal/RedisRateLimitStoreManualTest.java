package com.github.alonsopelaezflores.ratelimit.internal;

import com.github.alonsopelaezflores.ratelimit.store.RateLimitStore;

public class RedisRateLimitStoreManualTest {
    public static void main(String[] args) {
        RateLimitStore store = new RedisRateLimitStore(RateLimitConstants.DEFAULT_REDIS_HOST, RateLimitConstants.DEFAULT_REDIS_PORT);

        for (int i = 1; i <= 12; i++) {
            boolean allowed = store.tryConsume("test-key", 10, 10, 60000);
            System.out.println("Attempt " + i + ": " + (allowed ? "ALLOWED" : "BLOCKED"));
        }
    }
}
