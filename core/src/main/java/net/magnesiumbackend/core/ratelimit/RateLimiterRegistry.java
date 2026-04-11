package net.magnesiumbackend.core.ratelimit;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class RateLimiterRegistry {
    private final ConcurrentHashMap<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    public RateLimiter getOrCreate(String name, Consumer<RateLimiter.Builder> configure) {
        return limiters.computeIfAbsent(name, k -> {
            RateLimiter.Builder b = RateLimiter.builder();
            configure.accept(b);
            return b.build();
        });
    }

    public RateLimiter get(String name) {
        RateLimiter limiter = limiters.get(name);
        if (limiter == null) throw new IllegalArgumentException("No rate limiter: " + name);
        return limiter;
    }
}