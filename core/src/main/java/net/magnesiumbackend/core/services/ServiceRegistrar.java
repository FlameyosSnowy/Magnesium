package net.magnesiumbackend.core.services;

import java.util.Map;
import java.util.function.Function;

/**
 * DSL surface for registering service factories inside {@code .services(...)}.
 *
 * <p>Provides a fluent API for registering services and their factories during
 * application configuration. Services are instantiated lazily on first access
 * through {@link ServiceContext#get(Class)}.</p>
 *
 * <p>Each service is registered with a factory function that receives a
 * {@link ServiceContext} to resolve dependencies. This enables proper
 * dependency injection and circular dependency detection.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * MagnesiumApplication.builder()
 *     .services(services -> services
 *         // Simple registration
 *         .register(DatabaseConfig.class, ctx -> loadDbConfig())
 *
 *         // With dependency injection
 *         .register(UserRepository.class, ctx -> {
 *             DatabaseConfig config = ctx.get(DatabaseConfig.class);
 *             return new UserRepository(config);
 *         })
 *
 *         // Service depending on other services
 *         .register(UserService.class, ctx -> {
 *             UserRepository repo = ctx.get(UserRepository.class);
 *             EventBus bus = ctx.eventBus();
 *             return new UserService(repo, bus);
 *         })
 *     )
 *     .build();
 * }</pre>
 *
 * @see ServiceContext
 * @see ServiceRegistry
 * @see net.magnesiumbackend.core.MagnesiumApplication.Builder#services
 */
public final class ServiceRegistrar {
    private final Map<Class<?>, Function<ServiceContext, ?>> factories;

    ServiceRegistrar(Map<Class<?>, Function<ServiceContext, ?>> factories) {
        this.factories = factories;
    }

    /**
     * Registers a service factory for the given type.
     *
     * <p>The factory will be called lazily when the service is first requested
     * via {@link ServiceContext#get(Class)}. The factory receives a
     * ServiceContext to resolve any dependencies.</p>
     *
     * @param type    the service type to register
     * @param factory the factory function that creates the service instance
     * @param <T>     the service type
     * @return this registrar for method chaining
     */
    public <T> ServiceRegistrar register(Class<T> type, Function<ServiceContext, T> factory) {
        factories.put(type, factory);
        return this;
    }

    Map<Class<?>, Function<ServiceContext, ?>> factories() {
        return factories;
    }
}