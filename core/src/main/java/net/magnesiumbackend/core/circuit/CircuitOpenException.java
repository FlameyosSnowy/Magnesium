package net.magnesiumbackend.core.circuit;

/**
 * Exception thrown when a request is rejected by an open circuit breaker.
 *
 * <p>This exception indicates that the circuit breaker is in the OPEN state
 * and is failing fast to prevent cascading failures. The calling code should
 * handle this gracefully, typically by returning a cached value, fallback
 * response, or service unavailable error.</p>
 *
 * <p>Example handling:</p>
 * <pre>{@code
 * try {
 *     return breaker.execute(() -> externalService.call());
 * } catch (CircuitOpenException e) {
 *     // Circuit is open, use fallback
 *     return cachedValue.orElse(ResponseEntity.status(503).body("Service temporarily unavailable"));
 * }
 * }</pre>
 *
 * @see CircuitBreaker
 * @see CircuitBreaker.State#OPEN
 */
public final class CircuitOpenException extends RuntimeException {
    /**
     * Creates a new circuit open exception.
     *
     * @param name the name of the circuit breaker that is open
     */
    public CircuitOpenException(String name) {
        super("Circuit breaker '" + name + "' is OPEN, downstream unavailable");
    }
}