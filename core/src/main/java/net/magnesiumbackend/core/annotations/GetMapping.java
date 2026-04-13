package net.magnesiumbackend.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Maps HTTP GET requests to a handler method.
 *
 * <p>The annotated method will be invoked when an HTTP GET request matches
 * the specified path pattern. GET requests are typically used to retrieve
 * resources and should be idempotent and safe (no side effects).</p>
 *
 * <p>Path patterns may include variables in curly braces, e.g., {@code "/users/{id}"}.
 * Method parameters can be bound to these variables using {@link PathParam}.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @RestController
 * public class UserController {
 *     @GetMapping(path = "/users")
 *     public ResponseEntity<List<User>> listUsers() {
 *         return ResponseEntity.ok(userService.findAll());
 *     }
 *
 *     @GetMapping(path = "/users/{id}")
 *     public ResponseEntity<User> getUser(@PathParam String id) {
 *         return ResponseEntity.ok(userService.findById(id));
 *     }
 * }
 * }</pre>
 *
 * @see RestController
 * @see PathParam
 * @see PostMapping
 * @see PutMapping
 * @see DeleteMapping
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface GetMapping {
    /**
     * The path pattern for this route.
     *
     * <p>May include path variables in curly braces, e.g., {@code "/users/{id}"}.
     * Exact paths like {@code "/health"} are also valid.</p>
     *
     * @return the route path pattern
     */
    String path();
}