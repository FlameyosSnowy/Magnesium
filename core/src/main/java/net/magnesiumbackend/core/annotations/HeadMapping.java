package net.magnesiumbackend.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Maps HTTP HEAD requests to a handler method.
 *
 * <p>The annotated method will be invoked when an HTTP HEAD request matches
 * the specified path pattern. HEAD requests are identical to GET requests but
 * return only headers without a response body. They are typically used to check
 * resource existence, modification times, or content length without downloading
 * the full resource.</p>
 *
 * <p>Path patterns may include variables in curly braces, e.g., {@code "/files/{filename}"}.
 * Method parameters can be bound to these variables using {@link PathParam}.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @RestController
 * public class FileController {
 *     @HeadMapping(path = "/files/{filename}")
 *     public ResponseEntity<Void> checkFileExists(@PathParam String filename) {
 *         boolean exists = fileService.exists(filename);
 *         return exists ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
 *     }
 * }
 * }</pre>
 *
 * @see RestController
 * @see PathParam
 * @see GetMapping
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface HeadMapping {
    /**
     * The path pattern for this route.
     *
     * <p>May include path variables in curly braces, e.g., {@code "/files/{name}"}.
     * Exact paths are also valid.</p>
     *
     * @return the route path pattern
     */
    String path();
}