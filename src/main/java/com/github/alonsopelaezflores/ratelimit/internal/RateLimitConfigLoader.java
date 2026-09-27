package com.github.alonsopelaezflores.ratelimit.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class RateLimitConfigLoader {

    private static final String PREFIX = RateLimitConstants.PROFILE_PREFIX;

    public Map<String, RateLimitConfig> load(String resourcePath) {
        Properties raw = loadRawProperties(resourcePath);
        Map<String, Map<String, String>> groupedByProfile = groupByProfile(raw);
        return buildConfigs(groupedByProfile);
    }
    private Map<String, Map<String, String>> groupByProfile(Properties raw) {
        Map<String, Map<String, String>> grouped = new HashMap<>();

        for (String key : raw.stringPropertyNames()) {
            if (!key.startsWith(PREFIX)) continue;

            String remainder = key.substring(PREFIX.length());
            int lastDot = remainder.lastIndexOf('.');
            String profileName = remainder.substring(0, lastDot);
            String property = remainder.substring(lastDot + 1);

            grouped
                    .computeIfAbsent(profileName, k -> new HashMap<>())
                    .put(property, raw.getProperty(key));
        }

        return grouped;
    }
    private Map<String, RateLimitConfig> buildConfigs(Map<String, Map<String, String>> groupedByProfile) {
        Map<String, RateLimitConfig> result = new HashMap<>();

        for (Map.Entry<String, Map<String, String>> entry : groupedByProfile.entrySet()) {
            String profileName = entry.getKey();
            result.put(profileName, buildConfig(profileName, entry.getValue()));
        }

        return result;
    }
    private RateLimitConfig buildConfig(String profileName, Map<String, String> props) {
        requireProperty(profileName, props, "capacity");
        requireProperty(profileName, props, "refillTokens");
        requireProperty(profileName, props, "refillPeriodMillis");

        return new RateLimitConfig(
                Integer.parseInt(props.get("capacity")),
                Integer.parseInt(props.get("refillTokens")),
                Long.parseLong(props.get("refillPeriodMillis"))
        );
    }
    private void requireProperty(String profileName, Map<String, String> props, String property) {
        if (!props.containsKey(property)) {
            throw new IllegalStateException(
                    "Profile '" + profileName + "' is missing required property: " + property
            );
        }
    }
    public Properties loadRawProperties(String resourcePath) {
        Properties raw = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("File not found: " + resourcePath);
            }
            raw.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Error reading " + resourcePath, e);
        }
        return raw;
    }
    public Properties loadRawPropertiesOrEmpty(String resourcePath) {
        try {
            return loadRawProperties(resourcePath);
        } catch (IllegalStateException e) {
            return new Properties();
        }
    }
}
