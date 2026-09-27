package com.github.alonsopelaezflores.testapp;

import com.github.alonsopelaezflores.ratelimit.RateLimitExceededException;
import com.github.alonsopelaezflores.ratelimit.internal.RateLimitAspect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RateLimitAspectTest {
    private AnnotationConfigApplicationContext context;
    private MyService service;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(AppConfig.class);
        service = context.getBean(MyService.class);
    }

    @Test
    void blocksAfterExceedingTheLimit() {
        for (int i = 0; i < 10; i++) {
            service.limitMethod();
        }
        assertThrows(RuntimeException.class, service::limitMethod);
    }
    @Test
    void blocksOnlyTheUserWhoExceedsTheLimit() {
        for (int i = 0; i < 3; i++) {
            service.login("usuario-A");
        }
        assertThrows(RateLimitExceededException.class, () -> service.login("usuario-A"));

        assertDoesNotThrow(() -> service.login("usuario-B"));
    }
    @Test
    void profilesWithTheSameKeyDoNotShareBuckets() {
        String userId = "usuario-C-" + System.nanoTime();
        for (int i = 0; i < 3; i++) {
            service.login(userId);
        }
        assertThrows(RateLimitExceededException.class, () -> service.login(userId));

        assertDoesNotThrow(() -> service.pay(userId));
    }
    @Test
    void registersASingleAspectBean() {
        assertEquals(1, context.getBeansOfType(RateLimitAspect.class).size());
    }

    @AfterEach
    void tearDown() {
        context.close();
    }
}
