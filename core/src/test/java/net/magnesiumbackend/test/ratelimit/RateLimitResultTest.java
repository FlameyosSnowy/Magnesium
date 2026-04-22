package net.magnesiumbackend.test.ratelimit;

import net.magnesiumbackend.core.ratelimit.RateLimitResult;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RateLimitResultTest {

    @Test
    void allowedResult() {
        RateLimitResult result = RateLimitResult.allowed(5, 60, 10);
        assertTrue(result.allowed());
        assertEquals(5, result.remaining());
        assertEquals(60, result.resetAfterSeconds());
        assertEquals(10, result.limit());
    }

    @Test
    void deniedResult() {
        RateLimitResult result = RateLimitResult.denied(60, 10);
        assertFalse(result.allowed());
        assertEquals(0, result.remaining());
        assertEquals(60, result.resetAfterSeconds());
        assertEquals(10, result.limit());
    }

    @Test
    void resultEquality() {
        RateLimitResult r1 = RateLimitResult.allowed(5, 60, 10);
        RateLimitResult r2 = RateLimitResult.allowed(5, 60, 10);
        RateLimitResult r3 = RateLimitResult.denied(60, 10);

        assertEquals(r1, r2);
        assertNotEquals(r1, r3);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void resultComponents() {
        RateLimitResult result = new RateLimitResult(true, 3, 120, 5);
        assertTrue(result.allowed());
        assertEquals(3, result.remaining());
        assertEquals(120, result.resetAfterSeconds());
        assertEquals(5, result.limit());
    }
}
