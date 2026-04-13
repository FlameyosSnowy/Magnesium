package net.magnesiumbackend.core.annotations;

import net.magnesiumbackend.core.annotations.enums.RequiresMode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies required roles or authorities for accessing a controller.
 *
 * <p>This is a type-level annotation that applies to all routes within a controller.
 * For method-level permission control, use {@link Requires} instead.</p>
 *
 * <p>Secured implies authentication, so {@code @Authenticated} is not required
 * when using this annotation.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @RestController
 * @Secured("admin")
 * public class AdminController {
 *     // All routes require "admin" role
 * }
 * }</pre>
 *
 * <p>Example with multiple roles (requires ALL by default):</p>
 * <pre>{@code
 * @RestController
 * @Secured({"admin", "superuser"})
 * public class SuperAdminController {
 *     // All routes require BOTH "admin" AND "superuser" roles
 * }
 * }</pre>
 *
 * <p>Example with ANY mode:</p>
 * <pre>{@code
 * @RestController
 * @Secured(value = {"admin", "moderator"}, mode = RequiresMode.ANY)
 * public class ModeratorController {
 *     // All routes require either "admin" OR "moderator" role
 * }
 * }</pre>
 *
 * @see Requires
 * @see RequiresMode
 * @see Authenticated
 * @see Anonymous
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Secured {
    /**
     * The required roles or authorities.
     *
     * <p>The format depends on the security provider configuration.
     * Common formats include role names like "admin" or permission strings
     * like "resource:action".</p>
     *
     * @return the required role identifiers
     */
    String[] value();

    /**
     * The evaluation mode for multiple roles.
     *
     * <p>{@link RequiresMode#ALL} requires the user to have all listed roles.
     * {@link RequiresMode#ANY} requires the user to have at least one role.</p>
     *
     * @return the role check mode
     */
    RequiresMode mode() default RequiresMode.ALL;
}
