package net.magnesiumbackend.core.circuit;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Registry for managing multiple named circuit breakers.
 *
 * <p>Provides centralized access to circuit breakers, creating them on-demand
 * with configured settings. Thread-safe for concurrent access across multiple
 * requests or services.</p>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * CircuitBreakerRegistry registry = new CircuitBreakerRegistry();
 *
 * // Get or create a circuit breaker
 * CircuitBreaker paymentCB = registry.getOrCreate("payment-service", b ->
 *     b.failureThreshold(5).resetAfter(30_000L)
 * );
 *
 * // Use the circuit breaker
 * try {
 *     return paymentCB.execute(() -> paymentClient.charge(amount));
 * } catch (CircuitOpenException e) {
 *     return fallbackResponse();
 * }
 * }</pre>
 *
 * <p>Can be registered as a singleton service for application-wide circuit breaker management.</p>
 *
 * @see CircuitBreaker
 */
public final class CircuitBreakerRegistry {
    private final Map<String, CircuitBreaker> breakers = new ConcurrentHashMap<>();

    /**
     * Gets an existing circuit breaker or creates a new one with the given configuration.
     *
     * <p>If the circuit breaker already exists, it is returned as-is and the configure
     * callback is not invoked. Otherwise, a new circuit breaker is created using the
     * provided configuration.</p>
     *
     * @param name      the circuit breaker identifier
     * @param configure the configuration callback (only called when creating)
     * @return the circuit breaker instance
     */
    public CircuitBreaker getOrCreate(String name, Consumer<CircuitBreaker.Builder> configure) {
        return breakers.computeIfAbsent(name, k -> {
            CircuitBreaker.Builder builder = CircuitBreaker.builder(k);
            configure.accept(builder);
            return builder.build();
        });
    }

    public CircuitBreaker of(String name) { return getOrCreate(name, b -> {}); }

    public CircuitBreaker remove(String name) { return breakers.remove(name); }

    public void clear() { breakers.clear(); }

    public Map<String, CircuitBreaker> all() { return Collections.unmodifiableMap(breakers); }

    /**
     * Gets an existing circuit breaker by name.
     *
     * @param name the circuit breaker identifier
     * @return the circuit breaker instance
     */
    public CircuitBreaker get(String name) {
        return breakers.get(name);
    }

    /**
     * Returns a snapshot of all circuit breaker states.
     *
     * <p>Useful for monitoring and health checks.</p>
     *
     * @return an unmodifiable map of name to state
     */
    public Map<String, CircuitBreaker.State> states() {
        Map<String, CircuitBreaker.State> snapshot = new LinkedHashMap<>();
        breakers.forEach((k, v) -> snapshot.put(k, v.state()));
        return Collections.unmodifiableMap(snapshot);
    }
}