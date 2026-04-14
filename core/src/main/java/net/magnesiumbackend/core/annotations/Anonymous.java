package net.magnesiumbackend.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Opt-out of the default authentication requirement.
 *
 * <p>When the application has authentication enabled globally, routes and controllers
 * are protected by default. Use {@code @Anonymous} to explicitly allow unauthenticated
 * access to specific routes or entire controllers.</p>
 *
 * <p>This annotation takes precedence over global authentication settings but
 * can be combined with {@link RateLimit} to protect public endpoints from abuse.</p>
 *
 * <p>Example usage on a method:</p>
 * <pre>{@code
 * @RestController
 * @Authenticated
 * public class MixedController {
 *     @GetMapping(path = "/profile")
 *     public ResponseEntity<User> getProfile() {
 *         // Requires authentication
 *         return ResponseEntity.ok(currentUser());
 *     }
 *
 *     @GetMapping(path = "/health")
 *     @Anonymous
 *     public ResponseEntity<String> health() {
 *         // Publicly accessible
 *         return ResponseEntity.ok("UP");
 *     }
 * }
 * }</pre>
 *
 * <p>Example usage on a controller:</p>
 * <pre>{@code
 * @RestController
 * @Anonymous
 * public class PublicController {
 *     // All routes in this controller are publicly accessible
 * }
 * }</pre>
 *
 * @see Authenticated
 * @see Secured
 * @see RateLimit
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Anonymous {
}
