package net.magnesiumbackend.core.ratelimit;

public record RateLimitResult(
    boolean allowed,
    int     remaining,
    long    resetAfterSeconds,
    int     limit
) {
    public static RateLimitResult allowed(int remaining, long resetAfter, int limit) {
        return new RateLimitResult(true, remaining, resetAfter, limit);
    }
    public static RateLimitResult denied(long resetAfter, int limit) {
        return new RateLimitResult(false, 0, resetAfter, limit);
    }
}