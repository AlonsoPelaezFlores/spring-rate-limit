package com.github.alonsopelaezflores.ratelimit.internal;

import com.github.alonsopelaezflores.ratelimit.RateLimit;
import com.github.alonsopelaezflores.ratelimit.RateLimitExceededException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class RateLimitAspect {
    private final RateLimitService rateLimitService;
    private final RateLimitConfigRegistry rateLimitConfigRegistry;

    public RateLimitAspect(RateLimitService rateLimitService, RateLimitConfigRegistry rateLimitConfigRegistry) {
        this.rateLimitService = rateLimitService;
        this.rateLimitConfigRegistry = rateLimitConfigRegistry;
    }

    @Around("@annotation(ratelimit)")
    public Object applyLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        RateLimitConfig config = rateLimitConfigRegistry.getConfig(rateLimit.name());
        String key = resolveKey(joinPoint,rateLimit);
        boolean allowed = rateLimitService.isAllowed(key,config.getCapacity() , config.getRefillTokens(), config.getRefillPeriodMillis());
        if (!allowed) {
            throw new RateLimitExceededException("Rate limit exceeded for profile: " + rateLimit.name());
        }
        return joinPoint.proceed();
    }
    private String resolveKey(ProceedingJoinPoint joinPoint, RateLimit rateLimit) {
        int index = rateLimit.keyParamIndex();

        if (index == -1) {
            return rateLimit.name();
        }

        Object[] args = joinPoint.getArgs();
        if (index < 0 || index >= args.length) {
            throw new IllegalArgumentException(
                    "keyParamIndex=" + index + " is out of range for a method with " + args.length + " parameters"
            );
        }

        Object arg = args[index];
        if (arg == null) {
            throw new IllegalArgumentException("The parameter used as key (index " + index + ") is null");
        }
        return rateLimit.name() + ":" + arg;
    }
}
