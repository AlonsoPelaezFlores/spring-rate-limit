package com.github.alonsopelaezflores.ratelimit.internal;

public final class RateLimitConstants {
    private RateLimitConstants(){}

    public static final String PROPERTIES_FILE = "rate-limits.properties";
    public static final String PROFILE_PREFIX = "ratelimit.";

    public static final String STORE_TYPE_KEY = "store.type";
    public static final String STORE_TYPE_MEMORY = "memory";
    public static final String STORE_TYPE_REDIS = "redis";

    public static final String REDIS_HOST_KEY = "store.redis.host";
    public static final String REDIS_PORT_KEY = "store.redis.port";
    public static final String DEFAULT_REDIS_HOST = "localhost";
    public static final int DEFAULT_REDIS_PORT = 6379;

    public static final String LUA_SCRIPT_PATH = "rate_limit.lua";
}
