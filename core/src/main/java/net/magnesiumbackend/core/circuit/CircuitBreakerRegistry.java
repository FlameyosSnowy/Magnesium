package net.magnesiumbackend.core.circuit;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class CircuitBreakerRegistry {
    private final Map<String, CircuitBreaker> breakers = new ConcurrentHashMap<>();

    public CircuitBreaker getOrCreate(String name, Consumer<CircuitBreaker.Builder> configure) {
        return breakers.computeIfAbsent(name, k -> {
            CircuitBreaker.Builder builder = CircuitBreaker.builder(k);
            configure.accept(builder);
            return builder.build();
        });
    }

    public CircuitBreaker get(String name) {
        CircuitBreaker cb = breakers.get(name);
        if (cb == null) throw new IllegalArgumentException("No circuit breaker: " + name);
        return cb;
    }

    public Map<String, CircuitBreaker.State> states() {
        Map<String, CircuitBreaker.State> snapshot = new LinkedHashMap<>();
        breakers.forEach((k, v) -> snapshot.put(k, v.state()));
        return Collections.unmodifiableMap(snapshot);
    }
}