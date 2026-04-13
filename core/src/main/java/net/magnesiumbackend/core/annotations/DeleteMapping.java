package net.magnesiumbackend.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Maps HTTP DELETE requests to a handler method.
 *
 * <p>The annotated method will be invoked when an HTTP DELETE request matches
 * the specified path pattern. DELETE requests are used to remove resources.
 * DELETE is idempotent, meaning multiple identical requests should have the same
 * effect as a single request (deleting a non-existent resource returns success).</p>
 *
 * <p>Path patterns may include variables in curly braces, e.g., {@code "/users/{id}"}.
 * Method parameters can be bound to these variables using {@link PathParam}.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @RestController
 * public class UserController {
 *     @DeleteMapping(path = "/users/{id}")
 *     public ResponseEntity<Void> deleteUser(@PathParam String id) {
 *         userService.delete(id);
 *         return ResponseEntity.noContent().build();
 *     }
 * }
 * }</pre>
 *
 * @see RestController
 * @see PathParam
 * @see GetMapping
 * @see PostMapping
 * @see PutMapping
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface DeleteMapping {
    /**
     * The path pattern for this route.
     *
     * <p>May include path variables in curly braces, e.g., {@code "/users/{id}"}.
     * Exact paths are also valid.</p>
     *
     * @return the route path pattern
     */
    String path();
}