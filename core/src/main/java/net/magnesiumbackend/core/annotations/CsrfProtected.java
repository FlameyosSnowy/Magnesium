package net.magnesiumbackend.core.annotations;

import net.magnesiumbackend.core.security.CsrfConfig;
import net.magnesiumbackend.core.security.CsrfFilter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller or handler method as requiring CSRF protection.
 *
 * <p>When applied to a class, all handler methods in that class require
 * CSRF token validation (except safe methods like GET which only get the cookie).</p>
 *
 * <p>When applied to a method, that specific method requires CSRF validation.</p>
 *
 * <h3>Usage Examples</h3>
 * <pre>{@code
 * // Protect all methods in controller
 * @RestController
 * @CsrfProtected
 * public class UserController {
 *     @PostMapping("/users")
 *     public User createUser(...) { ... }
 *
 *     @PutMapping("/users/{id}")
 *     public User updateUser(...) { ... }
 * }
 *
 * // Protect specific methods only
 * @RestController
 * public class OrderController {
 *     @CsrfProtected
 *     @PostMapping("/orders")
 *     public Order createOrder(...) { ... }
 *
 *     @GetMapping("/orders")  // No CSRF required
 *     public List<Order> listOrders(...) { ... }
 * }
 * }</pre>
 *
 * <p>Requires {@link CsrfFilter} to be registered in the application.</p>
 *
 * @see CsrfFilter
 * @see CsrfConfig
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CsrfProtected {

    /**
     * Whether to skip CSRF validation for this handler even if class is annotated.
     * Only applies when used on a method.
     *
     * @return true to skip CSRF for this method
     */
    boolean skip() default false;

    /**
     * Additional path patterns to exclude from CSRF protection for this handler.
     *
     * @return exclusion patterns
     */
    String[] excludePaths() default {};
}
