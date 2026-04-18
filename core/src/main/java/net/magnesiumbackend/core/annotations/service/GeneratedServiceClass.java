package net.magnesiumbackend.core.annotations.service;

import net.magnesiumbackend.core.MagnesiumRuntime;
import net.magnesiumbackend.core.services.ServiceRegistrar;
import net.magnesiumbackend.core.services.ServiceRegistry;

/**
 * Contract implemented by compile-time generated service wiring code.
 *
 * <p>For each class annotated with {@code @RestService}, the annotation processor generates
 * a class implementing this interface. The generated class is responsible for:
 * <ul>
 *   <li>Creating the service instance with all dependencies resolved</li>
 *   <li>Registering the service with the {@link ServiceRegistry}</li>
 *   <li>Handling optional dependencies gracefully</li>
 * </ul>
 *
 * <p>Generated implementations are discovered via Java's {@link java.util.ServiceLoader}
 * mechanism and loaded by {@link net.magnesiumbackend.core.MagnesiumBootstrap}.
 *
 * <h3>Generated Code Example</h3>
 * <p>For a service like:</p>
 * <pre>{@code
 * @RestService
 * public class OrderService {
 *     public OrderService(DatabaseService db, @Inject(optional=true) NotificationService notify) { ... }
 * }
 * }</pre>
 *
 * <p>The processor generates:</p>
 * <pre>{@code
 * public class OrderService__ServiceRegistration implements GeneratedServiceClass {
 *     @Override
 *     public void register(MagnesiumRuntime runtime, ServiceRegistry services) {
 *         DatabaseService db = services.get(DatabaseService.class);
 *         NotificationService notify = resolveOptional(services);
 *         OrderService instance = new OrderService(db, notify);
 *         services.registerInstance(OrderService.class, instance);
 *     }
 *
 *     private NotificationService resolveOptional(ServiceRegistry services) {
 *         try {
 *             return services.get(NotificationService.class);
 *         } catch (Exception e) {
 *             return null;
 *         }
 *     }
 * }
 * }</pre>
 *
 * @see net.magnesiumbackend.core.annotations.RestService
 * @see net.magnesiumbackend.core.MagnesiumBootstrap
 * @see ServiceRegistry
 */
public interface GeneratedServiceClass {

    /**
     * Registers the service with the given runtime and service registry.
     *
     * <p>This method is called during application bootstrap to instantiate the service
     * and register it with the dependency injection container.
     *
     * @param runtime  the application runtime
     * @param services the service registry to register with
     */
    void register(MagnesiumRuntime runtime, ServiceRegistrar services);

    /**
     * Returns the service class being registered.
     *
     * <p>Used for ordering and introspection during bootstrap.
     *
     * @return the service class
     */
    Class<?> serviceType();

    /**
     * Returns the lifecycle priority for ordering.
     *
     * <p>Lower values are initialized first. Services are sorted by this priority
     * before registration.
     *
     * @return priority value (default: 0)
     */
    default int priority() {
        return 0;
    }

    /**
     * Returns the service scope.
     *
     * <p>Defines the instantiation scope of the service:
     * <ul>
     *   <li>SINGLETON - Single instance shared across the application (default)</li>
     *   <li>PROTOTYPE - New instance created for each injection point</li>
     *   <li>REQUEST - New instance per HTTP request</li>
     * </ul>
     *
     * @return the scope name (default: "SINGLETON")
     */
    default String scope() {
        return "SINGLETON";
    }
}
