package com.github.alonsopelaezflores.testapp;

import com.github.alonsopelaezflores.ratelimit.config.RateLimitAutoConfig;
import org.springframework.context.annotation.*;

@Configuration
@Import(RateLimitAutoConfig.class)
@ComponentScan(basePackages = "com.github.alonsopelaezflores")
public class AppConfig {
}
