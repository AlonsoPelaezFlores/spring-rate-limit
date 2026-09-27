package com.github.alonsopelaezflores.ratelimit.internal;

public class Bucket {
    private final long capacity;
    private long tokens;
    private final long refillTokens;
    private final long refillPeriodMillis;
    private long lastRefillTimestamp;
    public Bucket(long capacity, long refillTokens, long refillPeriodMillis) {
        this.capacity = capacity;
        this.tokens = capacity;
        this.refillTokens = refillTokens;
        this.refillPeriodMillis = refillPeriodMillis;
        this.lastRefillTimestamp = System.currentTimeMillis();
    }
    public synchronized boolean tryConsume() {
        refill();
        if (tokens > 0) {
            tokens--;
            return true;
        }
        return false;
    }
    private void refill() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRefillTimestamp;
        long tokensToAdd = (elapsed * refillTokens)/refillPeriodMillis;
        if (tokensToAdd > 0) {
            tokens = Math.min(capacity, tokens + tokensToAdd);
            lastRefillTimestamp = now;
        }
    }
}
