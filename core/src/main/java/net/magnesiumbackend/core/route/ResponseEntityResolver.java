package net.magnesiumbackend.core.route;

import net.magnesiumbackend.core.http.response.ResponseEntity;

import java.util.concurrent.CompletableFuture;

/**
 * Utility class for resolving async and sync return values from handlers and filters.
 *
 * <p>This class provides a unified way to handle both synchronous and asynchronous
 * operations by inspecting the return type at runtime.</p>
 *
 * <p>Supported return types:
 * <ul>
 *   <li>{@code ResponseEntity<T>} - Synchronous response</li>
 *   <li>{@code T} (any object) - Synchronous response, wrapped in ResponseEntity.ok()</li>
 *   <li>{@code CompletableFuture<ResponseEntity<T>>} - Asynchronous response</li>
 *   <li>{@code CompletableFuture<T>} - Asynchronous response, wrapped in ResponseEntity.ok()</li>
 * </ul>
 */
public final class ResponseEntityResolver {

    private ResponseEntityResolver() {
        // Utility class
    }

    /**
     * Resolves a handler/filter return value to a CompletableFuture.
     *
     * <p>If the value is already a CompletableFuture, it's returned as-is.
     * Otherwise, the value is wrapped in a completed CompletableFuture.</p>
     *
     * @param result The return value from a handler or filter
     * @return A CompletableFuture containing the resolved ResponseEntity
     */
    public static CompletableFuture<ResponseEntity<?>> toCompletableFuture(Object result) {
        if (result == null) {
            return CompletableFuture.completedFuture(ResponseEntity.ok(null));
        }

        if (result instanceof CompletableFuture<?> future) {
            return future.thenApply(ResponseEntityResolver::wrapIfNeeded);
        }

        return CompletableFuture.completedFuture(wrapIfNeeded(result));
    }

    /**
     * Wraps a value in ResponseEntity if it's not already a ResponseEntity.
     *
     * @param value The value to wrap
     * @return ResponseEntity containing the value
     */
    private static ResponseEntity<?> wrapIfNeeded(Object value) {
        if (value == null) {
            return ResponseEntity.ok(null);
        }
        if (value instanceof ResponseEntity<?> entity) {
            return entity;
        }
        return ResponseEntity.ok(value);
    }

    /**
     * Checks if a return type represents an async operation.
     *
     * @param result The return value to check
     * @return true if the value is a CompletableFuture
     */
    public static boolean isAsync(Object result) {
        return result instanceof CompletableFuture;
    }

    /**
     * Resolves a filter chain result synchronously.
     * Blocks if the result is async (use only in blocking contexts).
     *
     * @param result The return value from a filter
     * @return The resolved ResponseEntity
     * @throws IllegalStateException if async result cannot be resolved
     */
    public static ResponseEntity<?> resolveSync(Object result) {
        if (result instanceof CompletableFuture<?> future) {
            try {
                return (ResponseEntity<?>) future.get();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to resolve async filter result", e);
            }
        }
        return wrapIfNeeded(result);
    }
}
