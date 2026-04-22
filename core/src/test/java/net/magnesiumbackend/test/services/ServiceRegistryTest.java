package net.magnesiumbackend.test.services;

import net.magnesiumbackend.core.event.EventBus;
import net.magnesiumbackend.core.json.JsonProvider;
import net.magnesiumbackend.core.config.MagnesiumConfigurationManager;
import net.magnesiumbackend.core.config.ConfigSource;

import net.magnesiumbackend.core.services.ServiceContext;
import net.magnesiumbackend.core.services.ServiceRegistry;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.function.Function;

/**
 * Tests for ServiceRegistry.
 */
class ServiceRegistryTest {

    private ServiceRegistry createRegistry() {
        return createRegistry(new HashMap<>());
    }

    private ServiceRegistry createRegistry(HashMap<Class<?>, Function<ServiceContext, ?>> factories) {
        return new ServiceRegistry(
            factories,
            new EventBus(),
            new TestJsonProvider(),
            new MagnesiumConfigurationManager(ConfigSource.empty())
        );
    }

    @Test
    void getExistingService() {
        HashMap<Class<?>, Function<ServiceContext, ?>> factories = new HashMap<>();
        String service = "test-service";
        factories.put(String.class, ctx -> service);
        ServiceRegistry registry = createRegistry(factories);

        assertEquals(service, registry.get(String.class));
    }

    @Test
    void getNonExistingServiceThrows() {
        ServiceRegistry registry = createRegistry();
        assertThrows(IllegalArgumentException.class, () ->
            registry.get(String.class)
        );
    }

    @Test
    void lazyInstantiation() {
        HashMap<Class<?>, Function<ServiceContext, ?>> factories = new HashMap<>();
        int[] callCount = {0};

        factories.put(String.class, ctx -> {
            callCount[0]++;
            return "instance";
        });
        ServiceRegistry registry = createRegistry(factories);

        assertEquals(0, callCount[0]); // Not instantiated yet

        String result1 = registry.get(String.class);
        assertEquals(1, callCount[0]);
        assertEquals("instance", result1);

        String result2 = registry.get(String.class);
        assertEquals(1, callCount[0]); // Same instance, no new call
        assertSame(result1, result2);
    }

    @Test
    void serviceWithDependencies() {
        HashMap<Class<?>, Function<ServiceContext, ?>> factories = new HashMap<>();

        factories.put(Integer.class, ctx -> 42);
        factories.put(String.class, ctx ->
            "value-" + ctx.get(Integer.class)
        );
        ServiceRegistry registry = createRegistry(factories);

        assertEquals("value-42", registry.get(String.class));
    }

    @Test
    void circularDependencyDetection() {
        HashMap<Class<?>, Function<ServiceContext, ?>> factories = new HashMap<>();

        // A depends on B, B depends on A
        factories.put(ServiceA.class, ctx ->
            new ServiceA(ctx.get(ServiceB.class))
        );
        factories.put(ServiceB.class, ctx ->
            new ServiceB(ctx.get(ServiceA.class))
        );
        ServiceRegistry registry = createRegistry(factories);

        assertThrows(IllegalStateException.class, () ->
            registry.get(ServiceA.class)
        );
    }

    @Test
    void serviceContextProvidesEventBus() {
        HashMap<Class<?>, Function<ServiceContext, ?>> factories = new HashMap<>();
        factories.put(String.class, ctx -> {
            assertNotNull(ctx.eventBus());
            return "ok";
        });
        ServiceRegistry registry = createRegistry(factories);

        registry.get(String.class);
    }

    @Test
    void replaceInstance() {
        HashMap<Class<?>, Function<ServiceContext, ?>> factories = new HashMap<>();
        factories.put(String.class, ctx -> "original");
        ServiceRegistry registry = createRegistry(factories);

        // First get creates the instance
        assertEquals("original", registry.get(String.class));

        // Replace with new instance
        registry.replaceInstance(String.class, "replaced");
        assertEquals("replaced", registry.get(String.class));
    }

    // Test helper classes
    static class ServiceA {
        ServiceB b;
        ServiceA(ServiceB b) { this.b = b; }
    }

    static class ServiceB {
        ServiceA a;
        ServiceB(ServiceA a) { this.a = a; }
    }

    static class TestJsonProvider implements JsonProvider {
        @Override
        public String toJson(Object value) {
            return value.toString();
        }

        @Override
        public byte[] toJsonBytes(Object value) {
            return value.toString().getBytes();
        }

        @Override
        public <T> T fromJson(String json, Class<T> type) {
            return null;
        }

        @Override
        public <T> T fromJson(byte[] json, Class<T> type) {
            return null;
        }
    }
}
