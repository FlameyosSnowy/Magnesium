package net.magnesiumbackend.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds a method parameter to an HTTP request header value.
 *
 * <p>Extracts the specified header from the request and binds it to the
 * annotated method parameter. Supports optional headers with default values.</p>
 *
 * <p>Header names are case-insensitive according to HTTP specification.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @RestController
 * public class ApiController {
 *     @GetMapping(path = "/api/resource")
 *     public ResponseEntity<Resource> getResource(
 *             @RequestHeader("X-API-Key") String apiKey,
 *             @RequestHeader(value = "X-Request-ID", required = false) String requestId,
 *             @RequestHeader(value = "Accept", defaultValue = "application/json") String accept) {
 *         // apiKey is required - throws if missing
 *         // requestId is optional - null if not present
 *         // accept defaults to "application/json" if not present
 *         return ResponseEntity.ok(resourceService.fetch(apiKey));
 *     }
 * }
 * }</pre>
 *
 * @see RestController
 * @see GetMapping
 * @see PathParam
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestHeader {
    /**
     * The name of the HTTP header to bind.
     *
     * <p>Header names are case-insensitive. Common headers include
     * "Authorization", "Content-Type", "Accept", "X-Request-ID", etc.</p>
     *
     * @return the header name
     */
    String value();

    /**
     * Whether this header is required.
     *
     * <p>If {@code true} and the header is missing, the request fails with
     * a 400 Bad Request. If {@code false}, the parameter receives {@code null}
     * or the {@code defaultValue} if specified.</p>
     *
     * @return true if the header is required (default: true)
     */
    boolean required() default true;

    /**
     * Default value to use when the header is not present and not required.
     *
     * <p>Only applies when {@code required = false}. An empty string means
     * no default value (parameter receives null).</p>
     *
     * @return the default value (default: empty string)
     */
    String defaultValue() default "";
}
