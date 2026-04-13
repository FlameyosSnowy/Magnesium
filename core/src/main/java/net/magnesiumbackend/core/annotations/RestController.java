package net.magnesiumbackend.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a REST controller, enabling route handler method detection.
 *
 * <p>Classes annotated with {@code @RestController} are scanned at compile time
 * for methods annotated with HTTP mapping annotations such as:
 * <ul>
 *   <li>{@link GetMapping}</li>
 *   <li>{@link PostMapping}</li>
 *   <li>{@link PutMapping}</li>
 *   <li>{@link DeleteMapping}</li>
 *   <li>{@link PatchMappings}</li>
 *   <li>{@link HeadMapping}</li>
 *   <li>{@link OptionsMapping}</li>
 *   <li>{@link TracesMapping}</li>
 *   <li>{@link ConnectMapping}</li>
 * </ul>
 *
 * <p>Detected routes are registered automatically via code generation.
 * The controller instance is resolved from the {@link net.magnesiumbackend.core.services.ServiceRegistry}
 * during request handling.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @RestController
 * public class UserController {
 *     private final UserService userService;
 *
 *     public UserController(UserService userService) {
 *         this.userService = userService;
 *     }
 *
 *     @GetMapping(path = "/users/{id}")
 *     public ResponseEntity<User> getUser(@PathParam String id) {
 *         return ResponseEntity.ok(userService.findById(id));
 *     }
 * }
 * }</pre>
 *
 * @see GetMapping
 * @see PostMapping
 * @see PutMapping
 * @see DeleteMapping
 * @see PathParam
 * @see net.magnesiumbackend.core.services.ServiceRegistry
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface RestController {
}
