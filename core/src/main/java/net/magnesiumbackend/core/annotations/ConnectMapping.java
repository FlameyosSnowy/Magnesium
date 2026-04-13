package net.magnesiumbackend.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Maps HTTP CONNECT requests to a handler method.
 *
 * <p>The annotated method will be invoked when an HTTP CONNECT request matches
 * the specified path pattern. CONNECT requests are used to establish a tunnel
 * to the server identified by the target resource, typically for use with
 * TLS/SSL tunneling through proxy servers (HTTP CONNECT method).</p>
 *
 * <p><strong>Note:</strong> CONNECT is rarely used in application code and is
 * primarily handled at the proxy layer. Most applications do not need to
 * implement CONNECT handlers.</p>
 *
 * <p>Path patterns may include variables in curly braces.
 * Method parameters can be bound to these variables using {@link PathParam}.</p>
 *
 * @see RestController
 * @see PathParam
 * @see <a href="https://tools.ietf.org/html/rfc7231#section-4.3.6">RFC 7231 - CONNECT</a>
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface ConnectMapping {
    /**
     * The path pattern for this route.
     *
     * <p>May include path variables in curly braces.
     * Exact paths are also valid.</p>
     *
     * @return the route path pattern
     */
    String path();
}
