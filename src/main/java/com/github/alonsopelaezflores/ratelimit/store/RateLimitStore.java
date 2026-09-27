package com.github.alonsopelaezflores.ratelimit.store;

public interface RateLimitStore {
    boolean tryConsume(String key, int capacity, int refillTokens, long refillPeriodMillis);
}
