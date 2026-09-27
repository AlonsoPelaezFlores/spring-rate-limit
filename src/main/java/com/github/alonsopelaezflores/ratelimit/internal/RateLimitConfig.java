package com.github.alonsopelaezflores.ratelimit.internal;

public class RateLimitConfig {
    private final int capacity;
    private final int refillTokens;
    private final long refillPeriodMillis;

    public RateLimitConfig(int capacity, int refillTokens, long refillPeriodMillis) {
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillPeriodMillis = refillPeriodMillis;
    }

    public int getCapacity() { return capacity; }
    public int getRefillTokens() { return refillTokens; }
    public long getRefillPeriodMillis() { return refillPeriodMillis; }
}
