package net.magnesiumbackend.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requires authentication for accessing a route or controller.
 *
 * <p>When applied, the framework verifies that the request contains valid
 * authentication credentials before allowing access. The exact verification
 * mechanism depends on the configured security providers.</p>
 *
 * <p>If authentication fails, a 401 Unauthorized response is returned.
 * Use {@link Requires} for additional authorization checks
 * after authentication.</p>
 *
 * <p>Example usage on a method:</p>
 * <pre>{@code
 * @RestController
 * public class UserController {
 *     @GetMapping(path = "/users/{id}")
 *     @Authenticated
 *     public ResponseEntity<User> getUser(@PathParam String id) {
 *         return ResponseEntity.ok(userService.findById(id));
 *     }
 * }
 * }</pre>
 *
 * <p>Example usage on a controller (requires auth for all routes):</p>
 * <pre>{@code
 * @RestController
 * @Authenticated
 * public class SecureController {
 *     // All routes require authentication
 * }
 * }</pre>
 *
 * @see Anonymous
 * @see Requires
 * @see RateLimit
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Authenticated {
}