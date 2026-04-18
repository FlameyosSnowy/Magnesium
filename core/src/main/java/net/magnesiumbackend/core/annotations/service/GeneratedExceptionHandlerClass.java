package net.magnesiumbackend.core.annotations.service;

import net.magnesiumbackend.core.MagnesiumRuntime;
import net.magnesiumbackend.core.services.ServiceRegistry;

/**
 * Implemented by every compile-time-generated exception handler registration class.
 *
 * <p>The annotation processor generates one concrete implementation per
 * {@code @ExceptionHandler}-annotated class found in the project. Each
 * generated class receives the live {@link MagnesiumRuntime} and
 * {@link ServiceRegistry} so it can resolve handler constructor dependencies
 * via {@code serviceRegistry.get(SomeService.class)} at registration time.
 */
public interface GeneratedExceptionHandlerClass {
    void register(MagnesiumRuntime application, ServiceRegistry serviceRegistry);
}