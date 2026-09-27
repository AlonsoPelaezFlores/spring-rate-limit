package com.github.alonsopelaezflores.ratelimit.internal;

import com.github.alonsopelaezflores.ratelimit.store.RateLimitStore;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RedisRateLimitStore implements RateLimitStore {
    private final JedisPool jedisPool;
    private final String scriptSha;

    public RedisRateLimitStore(String host, int port) {
        this.jedisPool = new JedisPool(new JedisPoolConfig(),host,port);
        String script = loadScript();
        try(Jedis jedis = jedisPool.getResource()) {
            this.scriptSha = jedis.scriptLoad(script);
        }
    }

    private String loadScript() {
        String scriptLuaPath = RateLimitConstants.LUA_SCRIPT_PATH;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(scriptLuaPath)) {
            if (in == null) {
                throw new IllegalStateException("Script not found: " + scriptLuaPath);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error reading " + scriptLuaPath, e);
        }
    }

    @Override
    public boolean tryConsume(String key, int capacity, int refillTokens, long refillPeriodMillis) {
        try(Jedis jedis = jedisPool.getResource()){
            List<String> keys = Collections.singletonList(key);
            List<String> args = List.of(
                    String.valueOf(capacity),
                    String.valueOf(refillTokens),
                    String.valueOf(refillPeriodMillis),
                    String.valueOf(System.currentTimeMillis())
            );
            Object result = jedis.evalsha(scriptSha,keys,args);
            return ((Long) result) ==1L;
        }
    }
}
