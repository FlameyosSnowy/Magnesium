package net.magnesiumbackend.core.services;

import net.magnesiumbackend.core.config.MagnesiumConfigurationManager;
import net.magnesiumbackend.core.event.EventBus;
import net.magnesiumbackend.core.json.JsonProvider;

/**
 * Provides access to registered services and framework singletons.
 *
 * <p>ServiceContext is the core dependency injection interface used throughout
 * the Magnesium framework. It provides a service locator pattern for retrieving
 * registered service instances by their type.</p>
 *
 * <p>The context is passed to service factories during application startup,
 * allowing services to resolve their dependencies. It is also available to
 * route handlers and event listeners for accessing services at runtime.</p>
 *
 * <p>Example usage in a service factory:</p>
 * <pre>{@code
 * MagnesiumApplication.builder()
 *     .services(services -> services
 *         .register(OrderService.class, ctx -> {
 *             // Resolve dependencies from context
 * DatabaseConfig config = ctx.get(DatabaseConfig.class);
 *             EventBus bus = ctx.eventBus();
 *             return new OrderService(config, bus);
 *         })
 *     )
 *     .build();
 * }</pre>
 *
 * @see ServiceRegistry
 * @see ServiceRegistrar
 * @see EventBus
 */
public interface ServiceContext {
    /**
     * Retrieves a service instance by its type.
     *
     * <p>If the service has not been instantiated yet, it will be created
     * using the registered factory and cached for subsequent calls.</p>
     *
     * @param type the service class or interface
     * @param <T>  the service type
     * @return the service instance
     * @throws IllegalArgumentException if no factory is registered for the type
     * @throws IllegalStateException    if a circular dependency is detected
     */
    <T> T get(Class<T> type);

    JsonProvider jsonProvider();

    /**
     * Returns the application's event bus.
     *
     * <p>The event bus can be used to publish events or register subscribers
     * programmatically at runtime.</p>
     *
     * @return the event bus instance
     */
    EventBus eventBus();

    MagnesiumConfigurationManager configurationManager();
}