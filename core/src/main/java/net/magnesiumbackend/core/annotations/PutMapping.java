package net.magnesiumbackend.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Maps HTTP PUT requests to a handler method.
 *
 * <p>The annotated method will be invoked when an HTTP PUT request matches
 * the specified path pattern. PUT requests are typically used to update
 * existing resources or create a resource at a specific URI. PUT is idempotent,
 * meaning multiple identical requests should have the same effect as a single request.</p>
 *
 * <p>Path patterns may include variables in curly braces, e.g., {@code "/users/{id}"}.
 * Method parameters can be bound to these variables using {@link PathParam}.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @RestController
 * public class UserController {
 *     @PutMapping(path = "/users/{id}")
 *     public ResponseEntity<User> updateUser(@PathParam String id, @Body UserUpdateRequest request) {
 *         User updated = userService.update(id, request);
 *         return ResponseEntity.ok(updated);
 *     }
 * }
 * }</pre>
 *
 * @see RestController
 * @see PathParam
 * @see GetMapping
 * @see PostMapping
 * @see PatchMappings
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface PutMapping {
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