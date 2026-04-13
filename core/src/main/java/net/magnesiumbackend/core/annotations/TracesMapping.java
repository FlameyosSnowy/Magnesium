package net.magnesiumbackend.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Maps HTTP TRACE requests to a handler method.
 *
 * <p>The annotated method will be invoked when an HTTP TRACE request matches
 * the specified path pattern. TRACE requests perform a message loop-back test
 * along the path to the target resource, echoing back the received request.
 *
 * <p><strong>Note:</strong> TRACE is rarely used in production APIs and is often
 * disabled for security reasons as it can expose sensitive header information.
 * Many servers return 405 Method Not Allowed for TRACE requests by default.</p>
 *
 * <p>Path patterns may include variables in curly braces.
 * Method parameters can be bound to these variables using {@link PathParam}.</p>
 *
 * @see RestController
 * @see PathParam
 * @see <a href="https://tools.ietf.org/html/rfc7231#section-4.3.8">RFC 7231 - TRACE</a>
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface TracesMapping {
    /**
     * The path pattern for this route.
     *
     * <p>May include path variables in curly braces, e.g., {@code "/debug/{endpoint}"}.
     * Exact paths are also valid.</p>
     *
     * @return the route path pattern
     */
    String path();
}
