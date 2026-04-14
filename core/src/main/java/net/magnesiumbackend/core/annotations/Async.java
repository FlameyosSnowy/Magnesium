package net.magnesiumbackend.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a route handler method as async-capable.
 * The method may return {@code CompletableFuture<ResponseEntity<T>>} or
 * {@code CompletableFuture<T>} for non-blocking execution.
 *
 * <p>When used with non-blocking transports (e.g., Netty), the handler
 * will be executed asynchronously. With blocking transports (e.g., Tomcat),
 * the handler may block the calling thread unless configured otherwise.</p>
 *
 * <pre>{@code
 * @GetMapping(path = "/users/{id}")
 * @Async
 * public CompletableFuture<User> getUser(@PathParam String id) {
 *     return userService.findAsync(id)
 *         .thenApply(ResponseEntity::ok);
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface Async {
}
