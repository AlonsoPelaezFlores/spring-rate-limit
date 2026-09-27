package com.github.alonsopelaezflores.ratelimit.internal;

import java.util.Map;

public class RateLimitConfigRegistry {
    private final Map<String, RateLimitConfig> profiles;

    public RateLimitConfigRegistry(Map<String, RateLimitConfig> profiles) {
        this.profiles = profiles;
    }

    public RateLimitConfig getConfig(String name) {
        RateLimitConfig config = profiles.get(name);
        if (config == null) {
            throw new IllegalArgumentException("There is no rate limit profile named: " + name);
        }
        return config;
    }
}
