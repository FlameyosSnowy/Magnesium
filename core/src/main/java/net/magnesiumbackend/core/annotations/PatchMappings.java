package net.magnesiumbackend.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Maps HTTP PATCH requests to a handler method.
 *
 * <p>The annotated method will be invoked when an HTTP PATCH request matches
 * the specified path pattern. PATCH requests are used to apply partial modifications
 * to a resource. Unlike PUT which typically replaces the entire resource, PATCH
 * applies a delta or set of changes.</p>
 *
 * <p>Path patterns may include variables in curly braces, e.g., {@code "/users/{id}"}.
 * Method parameters can be bound to these variables using {@link PathParam}.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @RestController
 * public class UserController {
 *     @PatchMappings(path = "/users/{id}")
 *     public ResponseEntity<User> patchUser(@PathParam String id, @Body UserPatchRequest patch) {
 *         User updated = userService.patch(id, patch);
 *         return ResponseEntity.ok(updated);
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
public @interface PatchMappings {
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