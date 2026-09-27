package com.github.alonsopelaezflores.redistestapp;

import com.github.alonsopelaezflores.ratelimit.RateLimitExceededException;
import com.github.alonsopelaezflores.ratelimit.internal.RateLimitConstants;
import com.github.alonsopelaezflores.testapp.AppConfig;
import com.github.alonsopelaezflores.testapp.MyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import redis.clients.jedis.Jedis;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RedisRateLimitAspectTest {
    private AnnotationConfigApplicationContext context;
    private MyService service;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(AppConfig.class);
        service = context.getBean(MyService.class);
    }

    @Test
    @Tag("integration")
    void blocksAfterExceedingTheLimitUsingRedis() {
        String uniqueKey = "redis-test-user-" + System.currentTimeMillis();
        for (int i = 0; i < 3; i++) {
            service.login(uniqueKey);
        }
        assertThrows(RateLimitExceededException.class, () -> service.login(uniqueKey));
    }

    @Test
    @Tag("integration")
    void storesKeysWithPrefixAndProfileName() {
        String uniqueKey = "redis-test-user-" + System.nanoTime();
        service.login(uniqueKey);
        try (Jedis jedis = new Jedis(RateLimitConstants.DEFAULT_REDIS_HOST, RateLimitConstants.DEFAULT_REDIS_PORT)) {
            assertTrue(jedis.exists(RateLimitConstants.REDIS_KEY_PREFIX + "login-endpoint:" + uniqueKey));
        }
    }

    @Test
    @Tag("integration")
    void recoversWhenRedisLosesTheScriptCache() {
        String uniqueKey = "redis-test-user-" + System.nanoTime();
        service.login(uniqueKey);
        try (Jedis jedis = new Jedis(RateLimitConstants.DEFAULT_REDIS_HOST, RateLimitConstants.DEFAULT_REDIS_PORT)) {
            jedis.scriptFlush();
        }
        assertDoesNotThrow(() -> service.login(uniqueKey));
    }

    @AfterEach
    void tearDown() {
        context.close();
    }
}
