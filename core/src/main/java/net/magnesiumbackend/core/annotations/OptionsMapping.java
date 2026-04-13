package net.magnesiumbackend.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Maps HTTP OPTIONS requests to a handler method.
 *
 * <p>The annotated method will be invoked when an HTTP OPTIONS request matches
 * the specified path pattern. OPTIONS requests are used to describe the communication
 * options for the target resource, such as supported HTTP methods (CORS preflight).
 *
 * <p>Path patterns may include variables in curly braces, e.g., {@code "/api/{resource}"}.
 * Method parameters can be bound to these variables using {@link PathParam}.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @RestController
 * public class ApiController {
 *     @OptionsMapping(path = "/api/users")
 *     public ResponseEntity<Void> userOptions() {
 *         return ResponseEntity.ok()
 *             .header("Allow", "GET, POST, PUT, DELETE, OPTIONS")
 *             .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE")
 *             .build();
 *     }
 * }
 * }</pre>
 *
 * @see RestController
 * @see PathParam
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface OptionsMapping {
    /**
     * The path pattern for this route.
     *
     * <p>May include path variables in curly braces, e.g., {@code "/api/{resource}"}.
     * Exact paths are also valid.</p>
     *
     * @return the route path pattern
     */
    String path();
}