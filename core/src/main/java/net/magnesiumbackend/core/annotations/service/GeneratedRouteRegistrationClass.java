package net.magnesiumbackend.core.annotations.service;

import net.magnesiumbackend.core.MagnesiumRuntime;
import net.magnesiumbackend.core.route.HttpRouteRegistry;
import net.magnesiumbackend.core.services.ServiceRegistry;

/**
 * Implemented by every compile-time-generated HTTP route registration class.
 *
 * <p>The annotation processor generates one concrete implementation per
 * {@code @RestController} class found in the project. Each generated class
 * registers the controller's {@code @GetMapping}, {@code @PostMapping}, etc.
 * methods with the application's {@link HttpRouteRegistry} at startup.</p>
 *
 * <p>The generated class is named {@code <ControllerClass>_magnesium_Routes}
 * and placed in the same package as the controller. It resolves controller
 * constructor dependencies from {@link ServiceRegistry} when instantiating
 * the controller.</p>
 *
 * <p>Route handlers are wrapped to support:</p>
 * <ul>
 *   <li>Dependency injection from {@link ServiceRegistry}</li>
 *   <li>Parameter binding ({@code @PathParam}, {@code @RequestHeader}, etc.)</li>
 *   <li>Exception handling via {@link net.magnesiumbackend.core.exceptions.ExceptionHandlerRegistry}</li>
 *   <li>Response serialization</li>
 * </ul>
 *
 * @see net.magnesiumbackend.core.annotations.RestController
 * @see net.magnesiumbackend.core.annotations.GetMapping
 * @see net.magnesiumbackend.core.annotations.PostMapping
 * @see net.magnesiumbackend.core.route.HttpRouteRegistry
 * @see ServiceRegistry
 */
public interface GeneratedRouteRegistrationClass {
    /**
     * Registers all HTTP routes defined in the associated controller.
     *
     * <p>This method is called once during application startup. It instantiates
     * the controller (resolving constructor dependencies from serviceRegistry),
     * and registers each route handler with the httpRouteRegistry.</p>
     *
     * @param application       the running application
     * @param serviceRegistry   used to resolve controller constructor dependencies
     * @param httpRouteRegistry destination for route registrations
     */
    void register(MagnesiumRuntime application, ServiceRegistry serviceRegistry, HttpRouteRegistry httpRouteRegistry);
}
