package net.magnesiumbackend.core.circuit;

public final class CircuitOpenException extends RuntimeException {
    public CircuitOpenException(String name) {
        super("Circuit breaker '" + name + "' is OPEN, downstream unavailable");
    }
}