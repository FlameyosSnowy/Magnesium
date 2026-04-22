package net.magnesiumbackend.test.circuit;

import net.magnesiumbackend.core.circuit.CircuitBreaker;
import net.magnesiumbackend.core.circuit.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CircuitBreakerRegistry.
 */
class CircuitBreakerRegistryTest {

    @Test
    void createNewBreaker() {
        CircuitBreakerRegistry registry = new CircuitBreakerRegistry();
        CircuitBreaker breaker = registry.of("test");

        assertNotNull(breaker);
        assertEquals("test", breaker.name());
    }

    @Test
    void sameNameReturnsSameInstance() {
        CircuitBreakerRegistry registry = new CircuitBreakerRegistry();
        CircuitBreaker breaker1 = registry.of("test");
        CircuitBreaker breaker2 = registry.of("test");

        assertSame(breaker1, breaker2);
    }

    @Test
    void differentNamesReturnDifferentInstances() {
        CircuitBreakerRegistry registry = new CircuitBreakerRegistry();
        CircuitBreaker breaker1 = registry.of("test1");
        CircuitBreaker breaker2 = registry.of("test2");

        assertNotSame(breaker1, breaker2);
    }

    @Test
    void getExistingBreaker() {
        CircuitBreakerRegistry registry = new CircuitBreakerRegistry();
        CircuitBreaker created = registry.of("test");
        CircuitBreaker retrieved = registry.get("test");

        assertSame(created, retrieved);
    }

    @Test
    void getNonExistingReturnsNull() {
        CircuitBreakerRegistry registry = new CircuitBreakerRegistry();
        assertNull(registry.get("nonexistent"));
    }

    @Test
    void removeBreaker() {
        CircuitBreakerRegistry registry = new CircuitBreakerRegistry();
        registry.of("test");

        CircuitBreaker removed = registry.remove("test");
        assertNotNull(removed);
        assertNull(registry.get("test"));
    }

    @Test
    void clearRemovesAll() {
        CircuitBreakerRegistry registry = new CircuitBreakerRegistry();
        registry.of("test1");
        registry.of("test2");

        registry.clear();

        assertNull(registry.get("test1"));
        assertNull(registry.get("test2"));
    }

    @Test
    void allReturnsUnmodifiableView() {
        CircuitBreakerRegistry registry = new CircuitBreakerRegistry();
        registry.of("test");

        // that is the test
        //noinspection DataFlowIssue
        assertThrows(UnsupportedOperationException.class, () ->
            registry.all().put("new", CircuitBreaker.builder("new").build())
        );
    }
}
