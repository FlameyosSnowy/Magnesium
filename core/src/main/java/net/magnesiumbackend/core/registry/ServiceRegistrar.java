package net.magnesiumbackend.core.registry;

import java.util.Map;
import java.util.function.Function;

/** DSL surface for registering service factories inside {@code .services(...)}. */
public final class ServiceRegistrar {
    private final Map<Class<?>, Function<ServiceContext, ?>> factories;

    public ServiceRegistrar(Map<Class<?>, Function<ServiceContext, ?>> factories) {
        this.factories = factories;
    }

    public <T> ServiceRegistrar register(Class<T> type, Function<ServiceContext, T> factory) {
        factories.put(type, factory);
        return this;
    }

    Map<Class<?>, Function<ServiceContext, ?>> factories() {
        return factories;
    }
}