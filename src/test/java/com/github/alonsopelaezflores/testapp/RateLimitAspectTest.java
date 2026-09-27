package com.github.alonsopelaezflores.testapp;

import com.github.alonsopelaezflores.ratelimit.RateLimitExceededException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RateLimitAspectTest {
    private AnnotationConfigApplicationContext context;
    private MiServicio service;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(AppConfig.class);
        service = context.getBean(MiServicio.class);
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

    @AfterEach
    void tearDown() {
        context.close();
    }
}
