package net.magnesiumbackend.core.ratelimit;

import java.time.Duration;
import java.util.ArrayDeque;

final class WindowState {
    private final int      limit;
    private final long     windowMs;
    private final RateLimiter.Algorithm algorithm;

    private final ArrayDeque<Long> timestamps = new ArrayDeque<>();
    private long windowStart = System.currentTimeMillis();
    private int  fixedCount  = 0;

    private double tokens;
    private long   lastRefill;

    WindowState(int limit, Duration window, RateLimiter.Algorithm algorithm) {
        this.limit     = limit;
        this.windowMs  = window.toMillis();
        this.algorithm = algorithm;
        this.tokens    = limit;
        this.lastRefill = System.currentTimeMillis();
    }

    synchronized RateLimitResult consume() {
        return switch (algorithm) {
            case FIXED_WINDOW   -> consumeFixed();
            case SLIDING_WINDOW -> consumeSliding();
            case TOKEN_BUCKET   -> consumeTokenBucket();
        };
    }

    private RateLimitResult consumeFixed() {
        long now = System.currentTimeMillis();
        if (now - windowStart >= windowMs) {
            windowStart = now;
            fixedCount  = 0;
        }
        if (fixedCount >= limit) {
            long reset = (windowMs - (now - windowStart)) / 1000;
            return RateLimitResult.denied(reset, limit);
        }
        fixedCount++;
        long reset = (windowMs - (now - windowStart)) / 1000;
        return RateLimitResult.allowed(limit - fixedCount, reset, limit);
    }

    private RateLimitResult consumeSliding() {
        long now      = System.currentTimeMillis();
        long cutoff   = now - windowMs;

        // Evict timestamps outside the window
        while (!timestamps.isEmpty() && timestamps.peekFirst() < cutoff) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= limit) {
            long oldest = timestamps.peekFirst();
            long reset  = (oldest + windowMs - now) / 1000;
            return RateLimitResult.denied(Math.max(0, reset), limit);
        }

        timestamps.addLast(now);
        int remaining = limit - timestamps.size();
        return RateLimitResult.allowed(remaining, windowMs / 1000, limit);
    }

    private RateLimitResult consumeTokenBucket() {
        long now     = System.currentTimeMillis();
        long elapsed = now - lastRefill;
        double refill = (elapsed / (double) windowMs) * limit;
        tokens    = Math.min(limit, tokens + refill);
        lastRefill = now;

        if (tokens < 1.0) {
            long reset = (long) ((1.0 - tokens) / limit * windowMs) / 1000;
            return RateLimitResult.denied(reset, limit);
        }

        tokens--;
        return RateLimitResult.allowed((int) tokens, windowMs / 1000, limit);
    }
}