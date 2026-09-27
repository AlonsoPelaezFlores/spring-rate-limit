package com.github.alonsopelaezflores.ratelimit.config;

import com.github.alonsopelaezflores.ratelimit.internal.*;
import com.github.alonsopelaezflores.ratelimit.store.RateLimitStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.Map;
import java.util.Properties;

@Configuration
@EnableAspectJAutoProxy
public class RateLimitAutoConfig {
    @Bean
    public Map<String, RateLimitConfig> rateLimitProfiles() {
        return new RateLimitConfigLoader().load(RateLimitConstants.PROPERTIES_FILE);
    }

    @Bean
    public RateLimitConfigRegistry rateLimitConfigRegistry(Map<String, RateLimitConfig> rateLimitProfiles) {
        return new RateLimitConfigRegistry(rateLimitProfiles);
    }
    @Bean
    public RateLimitStore rateLimitStore() {
        RateLimitConfigLoader configLoader = new RateLimitConfigLoader();
        Properties props = configLoader.loadRawPropertiesOrEmpty(RateLimitConstants.PROPERTIES_FILE);
        String storeType = props.getProperty(RateLimitConstants.STORE_TYPE_KEY,RateLimitConstants.STORE_TYPE_MEMORY);

        if (RateLimitConstants.STORE_TYPE_REDIS.equals(storeType)){
            String host = props.getProperty(RateLimitConstants.REDIS_HOST_KEY, RateLimitConstants.DEFAULT_REDIS_HOST);
            int port = Integer.parseInt(props.getProperty(RateLimitConstants.REDIS_PORT_KEY, String.valueOf(RateLimitConstants.DEFAULT_REDIS_PORT)));
            return new RedisRateLimitStore(host,port);
        }
        return new InMemoryRateLimitStore();
    }

    @Bean
    public RateLimitService rateLimitService(RateLimitStore rateLimitStore) {
        return new RateLimitService(rateLimitStore);
    }

    @Bean
    public RateLimitAspect rateLimitAspect(RateLimitService rateLimitService, RateLimitConfigRegistry registry) {
        return new RateLimitAspect(rateLimitService, registry);
    }


}
