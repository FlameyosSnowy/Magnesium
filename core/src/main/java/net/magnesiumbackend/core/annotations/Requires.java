package net.magnesiumbackend.core.annotations;

import net.magnesiumbackend.core.annotations.enums.RequiresMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies required permissions or roles for accessing a route or controller.
 *
 * <p>The framework checks that the authenticated user has the required permissions
 * before allowing access. The check mode can be configured to require ALL permissions
 * or ANY permission.</p>
 *
 * <p>Works in conjunction with {@link Authenticated} and integrates with the
 * security framework to enforce authorization rules.</p>
 *
 * <p>Example usage on a method:</p>
 * <pre>{@code
 * @RestController
 * public class AdminController {
 *     @GetMapping(path = "/admin/users")
 *     @Authenticated
 *     @Requires("admin:users:read")
 *     public ResponseEntity<List<User>> listUsers() {
 *         return ResponseEntity.ok(userService.findAll());
 *     }
 *
 *     @DeleteMapping(path = "/admin/users/{id}")
 *     @Authenticated
 *     @Requires({"admin:users:delete", "audit:write"})
 *     public ResponseEntity<Void> deleteUser(@PathParam String id) {
 *         userService.delete(id);
 *         return ResponseEntity.noContent().build();
 *     }
 * }
 * }</pre>
 *
 * <p>Example using ANY mode (user needs any one of the permissions):</p>
 * <pre>{@code
 * @RestController
 * @Authenticated
 * @Requires(value = {"admin", "moderator"}, mode = RequiresMode.ANY)
 * public class ModerationController {
 *     // Accessible to users with either "admin" OR "moderator" permission
 * }
 * }</pre>
 *
 * @see Authenticated
 * @see RequiresMode
 * @see Anonymous
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Requires {
    /**
     * The required permission or role identifiers.
     *
     * <p>These strings are interpreted by the configured security provider.
     * Common formats include {@code "resource:action"} or simple role names.</p>
     *
     * @return the required permissions
     */
    String[] value();

    /**
     * The evaluation mode for multiple permissions.
     *
     * <p>{@link RequiresMode#ALL} requires the user to have every listed permission.
     * {@link RequiresMode#ANY} requires the user to have at least one permission.</p>
     *
     * @return the permission check mode
     */
    RequiresMode mode() default RequiresMode.ALL;
}