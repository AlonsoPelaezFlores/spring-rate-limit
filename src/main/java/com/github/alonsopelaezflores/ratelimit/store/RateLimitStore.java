package com.github.alonsopelaezflores.ratelimit.store;

public interface RateLimitStore extends AutoCloseable {
    boolean tryConsume(String key, int capacity, int refillTokens, long refillPeriodMillis);

    @Override
    default void close() {
    }
}
