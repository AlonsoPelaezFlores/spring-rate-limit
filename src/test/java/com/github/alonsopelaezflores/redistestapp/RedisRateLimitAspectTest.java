package com.github.alonsopelaezflores.redistestapp;

import com.github.alonsopelaezflores.ratelimit.RateLimitExceededException;
import com.github.alonsopelaezflores.testapp.AppConfig;
import com.github.alonsopelaezflores.testapp.MiServicio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class RedisRateLimitAspectTest {
    private AnnotationConfigApplicationContext context;
    private MiServicio service;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(AppConfig.class);
        service = context.getBean(MiServicio.class);
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

    @AfterEach
    void tearDown() {
        context.close();
    }
}
