package net.magnesiumbackend.test.ratelimit;

import net.magnesiumbackend.core.ratelimit.RateLimitResult;
import net.magnesiumbackend.core.ratelimit.RateLimiter;
import net.magnesiumbackend.core.ratelimit.WindowState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Duration;

class WindowStateTest {

    @Test
    void fixedWindowAllowsRequestsWithinLimit() {
        WindowState state = new WindowState(5, Duration.ofSeconds(1), RateLimiter.Algorithm.FIXED_WINDOW);

        for (int i = 0; i < 5; i++) {
            RateLimitResult result = state.consume();
            assertTrue(result.allowed(), "Request " + i + " should be allowed");
        }
    }

    @Test
    void fixedWindowDeniesRequestsOverLimit() {
        WindowState state = new WindowState(3, Duration.ofSeconds(1), RateLimiter.Algorithm.FIXED_WINDOW);

        // Exhaust the limit
        for (int i = 0; i < 3; i++) state.consume();

        RateLimitResult result = state.consume();
        assertFalse(result.allowed());
    }

    @Test
    void fixedWindowResetsAfterWindowExpires() throws InterruptedException {
        WindowState state = new WindowState(1, Duration.ofMillis(50), RateLimiter.Algorithm.FIXED_WINDOW);

        state.consume(); // Exhaust
        assertFalse(state.consume().allowed()); // Denied

        Thread.sleep(60); // Wait for window to expire

        RateLimitResult result = state.consume();
        assertTrue(result.allowed()); // Should reset
    }

    @Test
    void slidingWindowAllowsRequestsWithinLimit() {
        WindowState state = new WindowState(5, Duration.ofSeconds(1), RateLimiter.Algorithm.SLIDING_WINDOW);

        for (int i = 0; i < 5; i++) {
            assertTrue(state.consume().allowed(), "Request " + i + " should be allowed");
        }
    }

    @Test
    void slidingWindowDeniesRequestsOverLimit() {
        WindowState state = new WindowState(2, Duration.ofSeconds(1), RateLimiter.Algorithm.SLIDING_WINDOW);

        state.consume();
        state.consume();

        assertFalse(state.consume().allowed());
    }

    @Test
    void slidingWindowRecoversAsOldRequestsExpire() throws InterruptedException {
        WindowState state = new WindowState(2, Duration.ofMillis(100), RateLimiter.Algorithm.SLIDING_WINDOW);

        state.consume(); // t=0
        Thread.sleep(60);
        state.consume(); // t=60

        Thread.sleep(50); // t=110, first request should now be outside window

        RateLimitResult result = state.consume();
        assertTrue(result.allowed());
    }

    @Test
    void tokenBucketAllowsRequestsWithTokens() {
        WindowState state = new WindowState(3, Duration.ofSeconds(1), RateLimiter.Algorithm.TOKEN_BUCKET);

        for (int i = 0; i < 3; i++) {
            assertTrue(state.consume().allowed());
        }
    }

    @Test
    void tokenBucketDeniesWhenEmpty() {
        WindowState state = new WindowState(2, Duration.ofSeconds(1), RateLimiter.Algorithm.TOKEN_BUCKET);

        state.consume();
        state.consume();

        assertFalse(state.consume().allowed());
    }

    @Test
    void tokenBucketRefillsOverTime() throws InterruptedException {
        WindowState state = new WindowState(2, Duration.ofMillis(200), RateLimiter.Algorithm.TOKEN_BUCKET);

        state.consume();
        state.consume();
        assertFalse(state.consume().allowed());

        Thread.sleep(120); // Refill 1 token (200ms window, 2 tokens)

        RateLimitResult result = state.consume();
        assertTrue(result.allowed());
    }

    @Test
    void remainingCountDecreases() {
        WindowState state = new WindowState(5, Duration.ofSeconds(1), RateLimiter.Algorithm.FIXED_WINDOW);

        RateLimitResult r1 = state.consume();
        assertEquals(4, r1.remaining());

        RateLimitResult r2 = state.consume();
        assertEquals(3, r2.remaining());
    }
}
