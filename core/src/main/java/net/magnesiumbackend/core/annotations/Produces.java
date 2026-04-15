package net.magnesiumbackend.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the content type (MIME type) that a controller method produces.
 *
 * <p>This annotation is used at compile time to:
 * <ul>
 *   <li>Set the Content-Type header in responses</li>
 *   <li>Optimize response serialization</li>
 *   <li>Enable content negotiation</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 * {@code
 * @GetMapping(path = "/users/{id}")
 * @Produces("application/json")
 * public User getUser(@PathParam("id") String id) {
 *     return userService.findById(id);
 * }
 * }
 * </pre>
 *
 * @see ResponseStatus
 * @see GetMapping
 * @see PostMapping
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Produces {

    /**
     * The content type (MIME type) produced by the annotated method.
     *
     * <p>Common values:</p>
     * <ul>
     *   <li>{@code "application/json"} - JSON content</li>
     *   <li>{@code "text/plain"} - Plain text</li>
     *   <li>{@code "text/html"} - HTML content</li>
     *   <li>{@code "application/xml"} - XML content</li>
     *   <li>{@code "application/octet-stream"} - Binary data</li>
     * </ul>
     *
     * @return the MIME type string
     */
    String value();
}
