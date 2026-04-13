package net.magnesiumbackend.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an exception class with a default HTTP response status.
 *
 * <p>When an exception with this annotation is thrown from a route handler
 * and not caught by an exception handler, the framework returns the
 * specified HTTP status code.</p>
 *
 * <p>This provides a declarative way to map domain exceptions to HTTP
 * responses without writing explicit exception handlers for each case.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @ResponseStatus
 * public class UserNotFoundException extends RuntimeException {
 *     private final String userId;
 *
 *     public UserNotFoundException(String userId) {
 *         super("User not found: " + userId);
 *         this.userId = userId;
 *     }
 *
 *     // The generated handler will return 404 Not Found
 * }
 * }</pre>
 *
 * <p>When thrown from a controller:</p>
 * <pre>{@code
 * @RestController
 * public class UserController {
 *     @GetMapping(path = "/users/{id}")
 *     public User getUser(@PathParam String id) {
 *         return userRepository.findById(id)
 *             .orElseThrow(() -> new UserNotFoundException(id));
 *         // Returns 404 automatically if user not found
 *     }
 * }
 * }</pre>
 *
 * @see ExceptionHandler
 * @see net.magnesiumbackend.core.exceptions.ExceptionHandlerRegistry
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface ResponseStatus {
}
