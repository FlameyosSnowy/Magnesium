package net.magnesiumbackend.test.services;

import net.magnesiumbackend.core.services.ServiceContext;
import net.magnesiumbackend.core.services.ServiceRegistrar;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.function.Function;

/**
 * Tests for ServiceRegistrar.
 */
class ServiceRegistrarTest {

    @Test
    void registerAddsFactory() {
        HashMap<Class<?>, Function<ServiceContext, ?>> factories = new HashMap<>();
        ServiceRegistrar registrar = new ServiceRegistrar(factories);

        registrar.register(String.class, ctx -> "value");
        assertTrue(factories.containsKey(String.class));
    }

    @Test
    void registerReturnsRegistrarForChaining() {
        HashMap<Class<?>, Function<ServiceContext, ?>> factories = new HashMap<>();
        ServiceRegistrar registrar = new ServiceRegistrar(factories);

        ServiceRegistrar result = registrar
            .register(String.class, ctx -> "s")
            .register(Integer.class, ctx -> 1);

        assertSame(registrar, result);
    }

    @Test
    void registerInstanceAddsDirectInstance() {
        HashMap<Class<?>, Function<ServiceContext, ?>> factories = new HashMap<>();
        ServiceRegistrar registrar = new ServiceRegistrar(factories);

        String instance = "singleton";
        registrar.registerInstance(String.class, instance);

        assertSame(instance, factories.get(String.class).apply(null));
    }

    @Test
    void registerInstanceReturnsRegistrarForChaining() {
        HashMap<Class<?>, Function<ServiceContext, ?>> factories = new HashMap<>();
        ServiceRegistrar registrar = new ServiceRegistrar(factories);

        ServiceRegistrar result = registrar
            .registerInstance(String.class, "s")
            .registerInstance(Integer.class, 1);

        assertSame(registrar, result);
    }

    @Test
    void factoriesReturnsInternalMap() {
        HashMap<Class<?>, Function<ServiceContext, ?>> factories = new HashMap<>();
        ServiceRegistrar registrar = new ServiceRegistrar(factories);

        var actualFactories = registrar.factories();
        assertNotNull(actualFactories);
        assertTrue(actualFactories.isEmpty());
    }

    @Test
    void registerMultipleTypes() {
        HashMap<Class<?>, Function<ServiceContext, ?>> factories = new HashMap<>();
        ServiceRegistrar registrar = new ServiceRegistrar(factories);

        registrar
            .register(String.class, ctx -> "s")
            .register(Integer.class, ctx -> 1)
            .register(Double.class, ctx -> 1.5);

        assertTrue(factories.containsKey(String.class));
        assertTrue(factories.containsKey(Integer.class));
        assertTrue(factories.containsKey(Double.class));
    }
}
