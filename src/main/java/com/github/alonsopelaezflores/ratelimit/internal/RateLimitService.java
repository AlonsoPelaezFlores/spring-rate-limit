package com.github.alonsopelaezflores.ratelimit.internal;

import com.github.alonsopelaezflores.ratelimit.store.RateLimitStore;

public class RateLimitService {

    private final RateLimitStore store;

    public RateLimitService(RateLimitStore store) {
        this.store = store;
    }

    public boolean isAllowed(String key, int capacity, int refillTokens, long refillPeriodMillis) {
        return store.tryConsume(key, capacity, refillTokens, refillPeriodMillis);
    }
}
