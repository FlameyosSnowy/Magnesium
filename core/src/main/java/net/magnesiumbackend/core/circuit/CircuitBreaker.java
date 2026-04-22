package net.magnesiumbackend.core.circuit;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
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
 *   <li><b>CLOSED</b> — Normal operation, requests pass through. Failures are counted.</li>
 *   <li><b>OPEN</b> — Failure threshold reached. All requests fail immediately with
 *       {@link CircuitOpenException} until reset timeout passes.</li>
 *   <li><b>HALF_OPEN</b> — After reset timeout, exactly one probe request is allowed.
 *       A success closes the circuit; a failure re-opens it. All other concurrent
 *       requests are rejected until the probe resolves.</li>
 * </ul>
 *
 * <h3>Concurrency</h3>
 * <p>State transitions are performed atomically via {@link AtomicReference}
 * compareAndSet. The HALF_OPEN state allows at most one probe request at a time;
 * concurrent callers that arrive while a probe is already in-flight receive a
 * {@link CircuitOpenException} rather than all racing through.</p>
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

    /**
     * Immutable snapshot of circuit state. All transitions go through CAS on
     * {@link #stateRef}, so there are no synchronized blocks and no torn reads.
     */
    private sealed interface Snapshot permits Snapshot.Closed, Snapshot.Open, Snapshot.HalfOpen {

        /** Normal operation, consecutive failures are being counted. */
        record Closed(int failures) implements Snapshot {}

        /**
         * Tripped, {@code openedAt} is the wall-clock millisecond at which
         * the circuit opened.
         */
        record Open(long openedAt) implements Snapshot {}

        /**
         * Recovery probe. {@code probeInFlight} is set to {@code true} while
         * a probe call is executing, ensuring only one thread gets through.
         */
        record HalfOpen(int successes, boolean probeInFlight) implements Snapshot {}
    }

    private final String name;
    private final int    failureThreshold;
    private final int    successThreshold;
    private final long   resetAfterMs;

    private final AtomicReference<Snapshot> stateRef =
        new AtomicReference<>(new Snapshot.Closed(0));

    /**
     * Total number of times this circuit has tripped since creation.
     * Never reset to zero, useful for alerting on repeated instability.
     */
    private final AtomicLong tripCount = new AtomicLong(0);

    private CircuitBreaker(Builder b) {
        this.name             = b.name;
        this.failureThreshold = b.failureThreshold;
        this.successThreshold = b.successThreshold;
        this.resetAfterMs     = b.resetAfterMs;
    }

    /**
     * Executes a supplier through the circuit breaker.
     *
     * <p>Behaviour depends on the current circuit state:
     * <ul>
     *   <li>CLOSED — executes the action, counting consecutive failures</li>
     *   <li>OPEN — throws {@link CircuitOpenException} immediately (fail-fast)</li>
     *   <li>HALF_OPEN — allows exactly one probe through; all other concurrent
     *       callers receive {@link CircuitOpenException}</li>
     * </ul>
     *
     * @param action the action to execute
     * @param <T>    the return type
     * @return the action result
     * @throws CircuitOpenException if the circuit is open or a probe is already
     *                              in-flight in the HALF_OPEN state
     * @throws RuntimeException     if the action itself throws
     */
    public <T> T execute(Supplier<T> action) {
        return switch (resolvedSnapshot()) {
            case Snapshot.Open     ignored -> throw new CircuitOpenException(name);
            case Snapshot.Closed   ignored -> executeClosed(action);
            case Snapshot.HalfOpen ignored -> executeHalfOpen(action);
        };
    }

    /**
     * Executes a runnable through the circuit breaker.
     *
     * <p>Convenience overload for actions that return no value.</p>
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
            // Reset consecutive-failure counter on any success.
            stateRef.updateAndGet(s -> s instanceof Snapshot.Closed
                ? new Snapshot.Closed(0)
                : s);
            return result;
        } catch (Exception e) {
            recordClosedFailure();
            throw e;
        }
    }

    private <T> T executeHalfOpen(Supplier<T> action) {
        // Claim the probe slot atomically. Only one caller wins;
        // all others are rejected while probeInFlight == true.
        while (true) {
            Snapshot current = stateRef.get();

            // State may have changed between resolvedSnapshot() and now.
            if (!(current instanceof Snapshot.HalfOpen(int successes, boolean probeInFlight))) {
                return execute(action); // re-evaluate with fresh state
            }
            if (probeInFlight) {
                throw new CircuitOpenException(name);
            }
            if (stateRef.compareAndSet(current, new Snapshot.HalfOpen(successes, true))) {
                break;
            }
            // CAS los, retry the loop.
        }

        try {
            T result = action.get();
            recordHalfOpenSuccess();
            return result;
        } catch (Exception e) {
            trip();
            throw e;
        }
    }

    /**
     * Returns the current snapshot, advancing OPEN -> HALF_OPEN via CAS if the
     * reset timeout has elapsed. Only one thread performs the transition.
     */
    private Snapshot resolvedSnapshot() {
        while (true) {
            Snapshot s = stateRef.get();
            if (s instanceof Snapshot.Open(long openedAt)
                && System.currentTimeMillis() - openedAt >= resetAfterMs) {
                Snapshot halfOpen = new Snapshot.HalfOpen(0, false);
                if (stateRef.compareAndSet(s, halfOpen)) {
                    return halfOpen;
                }
                continue; // another thread won the CAS, retry
            }
            return s;
        }
    }

    /** Records a failure while CLOSED; trips the circuit if the threshold is reached. */
    private void recordClosedFailure() {
        while (true) {
            Snapshot s = stateRef.get();
            if (!(s instanceof Snapshot.Closed(int failures))) return; // already tripped
            int next = failures + 1;
            if (next >= failureThreshold) {
                if (stateRef.compareAndSet(s, new Snapshot.Open(System.currentTimeMillis()))) {
                    tripCount.incrementAndGet();
                }
                return;
            }
            if (stateRef.compareAndSet(s, new Snapshot.Closed(next))) return;
        }
    }

    /** Records a success while HALF_OPEN; closes the circuit if the threshold is reached. */
    private void recordHalfOpenSuccess() {
        while (true) {
            Snapshot s = stateRef.get();
            if (!(s instanceof Snapshot.HalfOpen ho)) return;
            int next = ho.successes() + 1;
            if (next >= successThreshold) {
                if (stateRef.compareAndSet(s, new Snapshot.Closed(0))) return;
            } else {
                // Stay HALF_OPEN but release the probe slot for the next probe.
                if (stateRef.compareAndSet(s, new Snapshot.HalfOpen(next, false))) return;
            }
        }
    }

    /** Opens the circuit unconditionally. */
    private void trip() {
        stateRef.set(new Snapshot.Open(System.currentTimeMillis()));
        tripCount.incrementAndGet();
    }

    /**
     * Forces the circuit to CLOSED regardless of its current state.
     *
     * <p>Intended for use by health endpoints or admin APIs when an operator has
     * confirmed the downstream service is healthy and wants to restore traffic
     * without waiting for the reset timeout to elapse.</p>
     */
    public void forceClose() {
        stateRef.set(new Snapshot.Closed(0));
    }

    /**
     * Forces the circuit OPEN regardless of its current state.
     *
     * <p>Useful in tests or maintenance windows where traffic should be shed
     * immediately.</p>
     */
    public void forceOpen() {
        stateRef.set(new Snapshot.Open(System.currentTimeMillis()));
        tripCount.incrementAndGet();
    }

    /**
     * Returns the current {@link State} of this circuit breaker.
     *
     * @return CLOSED, OPEN, or HALF_OPEN
     */
    public State state() {
        return switch (resolvedSnapshot()) {
            case Snapshot.Closed   ignored -> State.CLOSED;
            case Snapshot.Open     ignored -> State.OPEN;
            case Snapshot.HalfOpen ignored -> State.HALF_OPEN;
        };
    }

    /** Returns the name of this circuit breaker. */
    public String name() { return name; }

    public boolean isOpen()     { return state() == State.OPEN; }
    public boolean isClosed()   { return state() == State.CLOSED; }
    public boolean isHalfOpen() { return state() == State.HALF_OPEN; }

    /**
     * Returns the current consecutive-failure count.
     *
     * <ul>
     *   <li>CLOSED — live count toward the failure threshold</li>
     *   <li>OPEN — returns {@code failureThreshold} (the value that caused the trip)</li>
     *   <li>HALF_OPEN — returns {@code 0}</li>
     * </ul>
     */
    public int failureCount() {
        return switch (stateRef.get()) {
            case Snapshot.Closed   c  -> c.failures();
            case Snapshot.Open     ignored -> failureThreshold;
            case Snapshot.HalfOpen ignored -> 0;
        };
    }

    /**
     * Returns the number of successful probe calls accumulated in HALF_OPEN,
     * or {@code 0} when the circuit is not in the HALF_OPEN state.
     */
    public int successCount() {
        return switch (stateRef.get()) {
            case Snapshot.HalfOpen ho -> ho.successes();
            default                   -> 0;
        };
    }

    /**
     * Returns the total number of times this circuit has tripped since it was
     * created. Never resets. Useful for alerting on repeated instability.
     */
    public long tripCount() { return tripCount.get(); }

    /** The three possible states of a circuit breaker. */
    public enum State {
        /** Normal operation, requests pass through. */
        CLOSED,
        /** Failing fast, requests are rejected immediately. */
        OPEN,
        /** Testing recovery, one probe request is allowed at a time. */
        HALF_OPEN
    }

    /**
     * Creates a builder for a {@link CircuitBreaker} with the given name.
     *
     * @param name a unique identifier for this circuit breaker
     * @return a new builder
     */
    public static Builder builder(String name) { return new Builder(name); }

    /** Builder for {@link CircuitBreaker} instances. */
    public static final class Builder {
        private final String name;
        private int  failureThreshold = 5;
        private int  successThreshold = 2;
        private long resetAfterMs     = 30_000L;

        private Builder(String name) { this.name = name; }

        /**
         * Number of consecutive failures in CLOSED state before opening the circuit.
         * Defaults to {@code 5}.
         */
        public Builder failureThreshold(int v) { this.failureThreshold = v; return this; }

        /**
         * Number of consecutive successes in HALF_OPEN state required to close the
         * circuit again. Defaults to {@code 2}.
         */
        public Builder successThreshold(int v) { this.successThreshold = v; return this; }

        /**
         * Milliseconds to remain OPEN before transitioning to HALF_OPEN.
         * Defaults to {@code 30_000} (30 seconds).
         */
        public Builder resetAfter(long ms) { this.resetAfterMs = ms; return this; }

        /** Builds and returns the configured {@link CircuitBreaker}. */
        public CircuitBreaker build() { return new CircuitBreaker(this); }
    }
}