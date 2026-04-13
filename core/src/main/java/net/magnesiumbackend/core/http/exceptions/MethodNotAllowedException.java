package net.magnesiumbackend.core.http.exceptions;

import net.magnesiumbackend.core.http.response.HttpStatusCode;

/**
 * Exception for 405 Method Not Allowed errors.
 *
 * <p>Indicates the request method is known by the server but is not supported
 * by the target resource. The response should include an Allow header with
 * the supported methods.</p>
 *
 * <p>Usually thrown by the framework's routing layer rather than application code.</p>
 *
 * @see HttpExceptionBase
 */
public class MethodNotAllowedException extends HttpExceptionBase {

    /**
     * Creates a MethodNotAllowedException with the given message.
     *
     * @param message the error description
     */
    public MethodNotAllowedException(String message) {
        super(HttpStatusCode.of(405), message);
    }
}