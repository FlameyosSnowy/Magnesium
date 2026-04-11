package net.magnesiumbackend.core.circuit;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public final class CircuitBreaker {
    public enum State { CLOSED, OPEN, HALF_OPEN }

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
     * Execute a supplier through the circuit breaker.
     * Throws CircuitOpenException immediately if OPEN and reset window hasn't passed.
     */
    public <T> T execute(Supplier<T> action) {
        return switch (currentState()) {
            case OPEN      -> throw new CircuitOpenException(name);
            case HALF_OPEN -> executeHalfOpen(action);
            case CLOSED    -> executeClosed(action);
        };
    }

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

    public State state()  { return currentState(); }
    public String name()  { return name; }

    public static Builder builder(String name) { return new Builder(name); }

    public static final class Builder {
        private final String name;
        private int  failureThreshold = 5;
        private int  successThreshold = 2;
        private long resetAfterMs     = 30_000L;

        private Builder(String name) { this.name = name; }

        public Builder failureThreshold(int v)  { this.failureThreshold = v; return this; }
        public Builder successThreshold(int v)  { this.successThreshold = v; return this; }
        public Builder resetAfter(long ms)      { this.resetAfterMs = ms; return this; }
        public CircuitBreaker build()           { return new CircuitBreaker(this); }
    }
}