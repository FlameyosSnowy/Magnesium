package net.magnesiumbackend.core.circuit;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Circuit breaker for protecting against cascading failures.
 *
 * <p>The circuit breaker pattern prevents repeated calls to failing services by
 * temporarily blocking requests when failure rates exceed a threshold. This allows
 * the failing service time to recover while preventing resource exhaustion in
 * the calling service.</p>
 *
 * <h3>States</h3>
 * <ul>
 *   <li><b>CLOSED</b> - Normal operation, requests pass through. Failures are counted.</li>
 *   <li><b>OPEN</b> - Failure threshold reached. All requests fail immediately with
 *       {@link CircuitOpenException} until reset timeout passes.</li>
 *   <li><b>HALF_OPEN</b> - After reset timeout, limited test requests are allowed.
 *       Successes close the circuit, failures re-open it.</li>
 * </ul>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * CircuitBreaker breaker = CircuitBreaker.builder("payment-service")
 *     .failureThreshold(5)      // Open after 5 consecutive failures
 *     .successThreshold(2)      // Close after 2 successes in half-open
 *     .resetAfter(30_000L)      // Wait 30s before trying again
 *     .build();
 *
 * // Wrap calls to the protected service
 * try {
 *     PaymentResult result = breaker.execute(() -> paymentService.charge(amount));
 * } catch (CircuitOpenException e) {
 *     // Circuit is open, fail fast
 *     return ResponseEntity.status(503).body("Payment service unavailable");
 * } catch (PaymentException e) {
 *     // Call went through but failed, counted toward threshold
 *     return ResponseEntity.status(502).body("Payment failed");
 * }
 * }</pre>
 *
 * @see CircuitBreakerRegistry
 * @see CircuitOpenException
 */
public final class CircuitBreaker {

    /** Circuit breaker states. */
    public enum State {
        /** Normal operation - requests pass through. */
        CLOSED,
        /** Failing fast - requests rejected immediately. */
        OPEN,
        /** Testing recovery - limited requests allowed. */
        HALF_OPEN
    }

    private final String name;
    private final int    failureThreshold;   // failures before opening
    private final int    successThreshold;   // successes in HALF_OPEN before closing
    private final long   resetAfterMs;       // how long to stay OPEN

    private volatile State state         = State.CLOSED;
    private final AtomicInteger failures  = new AtomicInteger(0);
    private final AtomicInteger successes = new AtomicInteger(0);
    private volatile long openedAt       = 0L;

    private CircuitBreaker(Builder b) {
        this.name             = b.name;
        this.failureThreshold = b.failureThreshold;
        this.successThreshold = b.successThreshold;
        this.resetAfterMs     = b.resetAfterMs;
    }

    /**
     * Executes a supplier through the circuit breaker.
     *
     * <p>Depending on the circuit state:
     * <ul>
     *   <li>CLOSED - Executes the action normally, counting failures</li>
     *   <li>OPEN - Throws {@link CircuitOpenException} immediately</li>
     *   <li>HALF_OPEN - Allows limited test requests to check if service recovered</li>
     * </ul>
     *
     * @param action the action to execute
     * @param <T>    the return type
     * @return the action result
     * @throws CircuitOpenException if the circuit is open
     * @throws RuntimeException     if the action throws
     */
    public <T> T execute(Supplier<T> action) {
        return switch (currentState()) {
            case OPEN      -> throw new CircuitOpenException(name);
            case HALF_OPEN -> executeHalfOpen(action);
            case CLOSED    -> executeClosed(action);
        };
    }

    /**
     * Executes a runnable through the circuit breaker.
     *
     * <p>Convenience wrapper for actions that don't return a value.</p>
     *
     * @param action the action to execute
     * @throws CircuitOpenException if the circuit is open
     * @throws RuntimeException     if the action throws
     */
    public void execute(Runnable action) {
        execute(() -> { action.run(); return null; });
    }

    private <T> T executeClosed(Supplier<T> action) {
        try {
            T result = action.get();
            failures.set(0); // reset on success
            return result;
        } catch (Exception e) {
            if (failures.incrementAndGet() >= failureThreshold) {
                trip();
            }
            throw e;
        }
    }

    private <T> T executeHalfOpen(Supplier<T> action) {
        try {
            T result = action.get();
            if (successes.incrementAndGet() >= successThreshold) {
                close();
            }
            return result;
        } catch (Exception e) {
            trip();
            throw e;
        }
    }

    private State currentState() {
        if (state == State.OPEN) {
            if (System.currentTimeMillis() - openedAt >= resetAfterMs) {
                // Attempt recovery
                state = State.HALF_OPEN;
                successes.set(0);
                return State.HALF_OPEN;
            }
        }
        return state;
    }

    private synchronized void trip() {
        state    = State.OPEN;
        openedAt = System.currentTimeMillis();
        failures.set(0);
    }

    private synchronized void close() {
        state = State.CLOSED;
        failures.set(0);
        successes.set(0);
    }

    /**
     * Returns the current state of this circuit breaker.
     *
     * @return the current state (CLOSED, OPEN, or HALF_OPEN)
     */
    public State state()  { return currentState(); }

    /**
     * Returns the name of this circuit breaker.
     *
     * @return the circuit breaker name
     */
    public String name()  { return name; }

    /**
     * Creates a builder for a circuit breaker with the given name.
     *
     * @param name the circuit breaker identifier
     * @return a new builder
     */
    public static Builder builder(String name) { return new Builder(name); }

    /**
     * Builder for {@link CircuitBreaker} instances.
     */
    public static final class Builder {
        private final String name;
        private int  failureThreshold = 5;
        private int  successThreshold = 2;
        private long resetAfterMs     = 30_000L;

        private Builder(String name) { this.name = name; }

        /**
         * Sets the number of consecutive failures before opening the circuit.
         *
         * @param v the failure threshold
         * @return this builder
         */
        public Builder failureThreshold(int v)  { this.failureThreshold = v; return this; }

        /**
         * Sets the number of consecutive successes in HALF_OPEN state before closing.
         *
         * @param v the success threshold
         * @return this builder
         */
        public Builder successThreshold(int v)  { this.successThreshold = v; return this; }

        /**
         * Sets the time in milliseconds to wait before transitioning from OPEN to HALF_OPEN.
         *
         * @param ms the reset timeout in milliseconds
         * @return this builder
         */
        public Builder resetAfter(long ms)      { this.resetAfterMs = ms; return this; }

        /**
         * Builds the circuit breaker with the configured settings.
         *
         * @return the configured circuit breaker
         */
        public CircuitBreaker build()           { return new CircuitBreaker(this); }
    }
}