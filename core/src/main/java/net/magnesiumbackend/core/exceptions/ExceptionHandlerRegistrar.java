package net.magnesiumbackend.core.exceptions;

import net.magnesiumbackend.core.http.ResponseEntity;
import net.magnesiumbackend.core.services.ServiceRegistrar;
import net.magnesiumbackend.core.route.RouteDefinition;
import net.magnesiumbackend.core.route.RouteExceptionHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Builder-facing API for registering exception handlers.
 *
 * <p>Intentionally mirrors the {@link ServiceRegistrar} pattern, a thin,
 * fluent façade over {@link ExceptionHandlerRegistry} that is handed to the
 * user's lambda in {@code MagnesiumApplication.Builder#exceptions()} and then
 * discarded. The user never holds a reference to the services directly.
 *
 * <pre>{@code
 * .exceptions(ex -> ex
 *     .global(ValidationException.class, (e, req) ->
 *         Response.status(400, e.getMessage()))
 *     .global(Exception.class, (e, req) ->
 *         Response.status(500, "Unexpected error"))
 *     .local(OrderController.class, NotFoundException.class, (e, req) ->
 *         Response.status(404, "Order not found"))
 *     .fallback((e, req) ->
 *         Response.status(500, "Unhandled: " + e.getMessage()))
 * )
 * }</pre>
 */
public final class ExceptionHandlerRegistrar {

    private final ExceptionHandlerRegistry registry;

    public ExceptionHandlerRegistrar(@NotNull ExceptionHandlerRegistry registry) {
        this.registry = registry;
    }

    /**
     * Registers a handler that applies to every controller in the application.
     *
     * @param exceptionType the exception class to handle
     * @param handler       receives the exception and the originating request;
     *                      must return a {@link ResponseEntity}
     */
    public <T extends Throwable> ExceptionHandlerRegistrar global(
        @NotNull Class<T> exceptionType,
        @NotNull RouteExceptionHandler handler
    ) {
        registry.registerGlobal(exceptionType, handler);
        return this;
    }

    /**
     * Registers a handler scoped to a single controller class.
     * Takes precedence over any global handler for the same exception type
     * when the exception originates inside {@code routeDefinition}.
     *
     * @param routeDefinition the controller class this handler is scoped to
     * @param exceptionType  the exception class to handle
     * @param handler        receives the exception and the originating request
     */
    public <T extends Throwable> ExceptionHandlerRegistrar local(
        @NotNull RouteDefinition routeDefinition,
        @NotNull Class<T> exceptionType,
        @NotNull RouteExceptionHandler handler
    ) {
        registry.registerRoute(routeDefinition, exceptionType, handler);
        return this;
    }

    /**
     * Sets the fallback handler invoked when no registered handler matches
     * the thrown exception. Replaces the built-in default 500 response.
     *
     * @param fallback must never throw
     */
    public ExceptionHandlerRegistrar fallback(
        @NotNull RouteExceptionHandler fallback
    ) {
        registry.setFallback(fallback);
        return this;
    }
}