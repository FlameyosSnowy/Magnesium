package net.magnesiumbackend.test.circuit;

import net.magnesiumbackend.core.circuit.CircuitBreaker;
import net.magnesiumbackend.core.circuit.CircuitOpenException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Duration;

/**
 * Tests for CircuitBreaker.
 */
class CircuitBreakerTest {

    @Test
    void builderCreatesCircuitBreaker() {
        CircuitBreaker breaker = CircuitBreaker.builder("test").build();
        assertNotNull(breaker);
        assertEquals("test", breaker.name());
    }

    @Test
    void defaultStateIsClosed() {
        CircuitBreaker breaker = CircuitBreaker.builder("test").build();
        assertEquals(CircuitBreaker.State.CLOSED, breaker.state());
        assertTrue(breaker.isClosed());
        assertFalse(breaker.isOpen());
    }

    @Test
    void successfulExecutionInClosedState() {
        CircuitBreaker breaker = CircuitBreaker.builder("test").build();
        String result = breaker.execute(() -> "success");
        assertEquals("success", result);
    }

    @Test
    void failuresAccumulate() {
        CircuitBreaker breaker = CircuitBreaker.builder("test")
            .failureThreshold(3)
            .build();

        // Record 2 failures
        for (int i = 0; i < 2; i++) {
            assertThrows(RuntimeException.class, () ->
                breaker.execute(() -> { throw new RuntimeException("fail"); })
            );
        }

        // Still closed
        assertEquals(CircuitBreaker.State.CLOSED, breaker.state());
    }

    @Test
    void opensAfterThresholdFailures() {
        CircuitBreaker breaker = CircuitBreaker.builder("test")
            .failureThreshold(3)
            .build();

        // Record 3 failures
        for (int i = 0; i < 3; i++) {
            assertThrows(RuntimeException.class, () ->
                breaker.execute(() -> { throw new RuntimeException("fail"); })
            );
        }

        assertEquals(CircuitBreaker.State.OPEN, breaker.state());
        assertTrue(breaker.isOpen());
    }

    @Test
    void openCircuitThrowsCircuitOpenException() {
        CircuitBreaker breaker = CircuitBreaker.builder("test")
            .failureThreshold(1)
            .build();

        // Open the circuit
        assertThrows(RuntimeException.class, () ->
            breaker.execute(() -> { throw new RuntimeException("fail"); })
        );

        assertEquals(CircuitBreaker.State.OPEN, breaker.state());

        assertThrows(CircuitOpenException.class, () ->
            breaker.execute(() -> "should not execute")
        );
    }

    @Test
    void circuitTransitionsToHalfOpenAfterTimeout() throws InterruptedException {
        CircuitBreaker breaker = CircuitBreaker.builder("test")
            .failureThreshold(1)
            .resetAfter(50) // 50ms
            .build();

        // Open the circuit
        assertThrows(RuntimeException.class, () ->
            breaker.execute(() -> { throw new RuntimeException("fail"); })
        );

        Thread.sleep(60); // Wait for timeout

        // Should be half-open
        assertEquals(CircuitBreaker.State.HALF_OPEN, breaker.state());
    }

    @Test
    void halfOpenSuccessClosesCircuit() throws InterruptedException {
        CircuitBreaker breaker = CircuitBreaker.builder("test")
            .failureThreshold(1)
            .successThreshold(1)
            .resetAfter(50)
            .build();

        // Open the circuit
        assertThrows(RuntimeException.class, () ->
            breaker.execute(() -> { throw new RuntimeException("fail"); })
        );

        Thread.sleep(60); // Wait for timeout -> half-open

        // Success in half-open should close circuit
        breaker.execute(() -> "success");

        assertEquals(CircuitBreaker.State.CLOSED, breaker.state());
    }

    @Test
    void halfOpenFailureReopensCircuit() throws InterruptedException {
        CircuitBreaker breaker = CircuitBreaker.builder("test")
            .failureThreshold(1)
            .resetAfter(50)
            .build();

        // Open the circuit
        assertThrows(RuntimeException.class, () ->
            breaker.execute(() -> { throw new RuntimeException("fail"); })
        );

        Thread.sleep(60); // Wait for timeout -> half-open

        // Failure in half-open should reopen circuit
        assertThrows(RuntimeException.class, () ->
            breaker.execute(() -> { throw new RuntimeException("fail again"); })
        );

        assertEquals(CircuitBreaker.State.OPEN, breaker.state());
    }

    @Test
    void successThresholdRequiredToClose() throws InterruptedException {
        CircuitBreaker breaker = CircuitBreaker.builder("test")
            .failureThreshold(1)
            .successThreshold(3)
            .resetAfter(50)
            .build();

        // Open the circuit
        assertThrows(RuntimeException.class, () ->
            breaker.execute(() -> { throw new RuntimeException("fail"); })
        );

        Thread.sleep(60); // Wait for timeout -> half-open

        // 2 successes - should stay half-open
        breaker.execute(() -> "ok");
        breaker.execute(() -> "ok");

        assertEquals(CircuitBreaker.State.HALF_OPEN, breaker.state());

        // 3rd success - should close
        breaker.execute(() -> "ok");

        assertEquals(CircuitBreaker.State.CLOSED, breaker.state());
    }

    @Test
    void recordsMetrics() throws InterruptedException {
        CircuitBreaker breaker = CircuitBreaker.builder("test")
            .failureThreshold(1)
            .successThreshold(2)
            .resetAfter(50)
            .build();

        assertEquals(0, breaker.failureCount());
        assertEquals(0, breaker.successCount());

        assertThrows(RuntimeException.class, () ->
            breaker.execute(() -> { throw new RuntimeException("fail"); })
        );

        Thread.sleep(60);

        // First probe — still HALF_OPEN, one success accumulated
        breaker.execute(() -> "ok");
        assertEquals(1, breaker.successCount());

        // Second probe — hits successThreshold, transitions to CLOSED
        breaker.execute(() -> "ok");

        // successCount() returns 0 when not in HALF_OPEN — the transition is the assertion
        assertEquals(CircuitBreaker.State.CLOSED, breaker.state());
        assertEquals(0, breaker.successCount());
    }

    @Test
    void currentStateMethod() {
        CircuitBreaker breaker = CircuitBreaker.builder("test").build();
        assertEquals(CircuitBreaker.State.CLOSED, breaker.state());
    }

    @Test
    void stateEnumValues() {
        assertEquals(3, CircuitBreaker.State.values().length);
        assertNotNull(CircuitBreaker.State.valueOf("CLOSED"));
        assertNotNull(CircuitBreaker.State.valueOf("OPEN"));
        assertNotNull(CircuitBreaker.State.valueOf("HALF_OPEN"));
    }
}
