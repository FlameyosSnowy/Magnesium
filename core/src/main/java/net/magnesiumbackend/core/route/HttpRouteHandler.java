package net.magnesiumbackend.core.route;

/**
 * Unified functional interface for HTTP route handlers supporting both sync and async operations.
 *
 * <p>This interface accepts handlers that return:
 * <ul>
 *   <li>{@code ResponseEntity<T>} - Synchronous response</li>
 *   <li>{@code T} (any object) - Synchronous response, auto-wrapped in ResponseEntity.ok()</li>
 *   <li>{@code CompletableFuture<ResponseEntity<T>>} - Asynchronous response</li>
 *   <li>{@code CompletableFuture<T>} - Asynchronous response, auto-wrapped in ResponseEntity.ok()</li>
 * </ul>
 *
 * <p>The actual return type is resolved at runtime using {@link ResponseEntityResolver}.</p>
 *
 * @see ResponseEntityResolver
 * @see RequestContext
 */
@FunctionalInterface
public interface HttpRouteHandler {
    /**
     * Handles the HTTP request.
     *
     * @param request The request context containing path variables, headers, body, etc.
     * @return The response (ResponseEntity, CompletableFuture, or raw value)
     */
    Object handle(RequestContext request);
}