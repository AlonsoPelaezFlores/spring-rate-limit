package com.github.alonsopelaezflores.ratelimit.internal;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RateLimitConfigLoaderTest {
    private final RateLimitConfigLoader loader = new RateLimitConfigLoader();

    @Test
    void parsesACompleteProfileCorrectly() {
        Map<String, RateLimitConfig> profiles = loader.load("rate-limits.properties");

        assertTrue(profiles.containsKey("test-endpoint"));
        RateLimitConfig config = profiles.get("test-endpoint");
        assertEquals(10, config.getCapacity());
        assertEquals(10, config.getRefillTokens());
        assertEquals(1000L, config.getRefillPeriodMillis());
    }

    @Test
    void throwsExceptionWhenRequiredPropertyIsMissing() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> loader.load("rate-limits-incomplete-test.properties"));

        assertTrue(exception.getMessage().contains("broken-profile"));
        assertTrue(exception.getMessage().contains("refillTokens"));
    }

    @Test
    void throwsExceptionWhenFileDoesNotExist() {
        assertThrows(IllegalStateException.class,
                () -> loader.load("nonexistent-file.properties"));
    }
}
