package net.magnesiumbackend.core.route;

/**
 * Unified functional interface for HTTP filters supporting both sync and async operations.
 *
 * <p>This interface accepts handlers that return:
 * <ul>
 *   <li>{@code ResponseEntity<T>} - Synchronous filter result</li>
 *   <li>{@code T} (any object) - Synchronous result, auto-wrapped in ResponseEntity.ok()</li>
 *   <li>{@code CompletableFuture<ResponseEntity<T>>} - Asynchronous filter result</li>
 *   <li>{@code CompletableFuture<T>} - Asynchronous result, auto-wrapped in ResponseEntity.ok()</li>
 * </ul>
 *
 * <p>The actual return type is resolved at runtime using {@link ResponseEntityResolver}.</p>
 *
 * @see ResponseEntityResolver
 * @see FilterChain
 */
@FunctionalInterface
public interface HttpFilter {
    /**
     * Handles the filter logic.
     *
     * @param request The request context
     * @param chain The filter chain to continue processing
     * @return The filter result (ResponseEntity, CompletableFuture, or raw value)
     */
    Object handle(RequestContext request, FilterChain chain);
}