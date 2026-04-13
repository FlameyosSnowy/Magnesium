package net.magnesiumbackend.core.http.exceptions;

import net.magnesiumbackend.core.http.response.HttpStatusCode;

/**
 * Exception for 401 Unauthorized errors.
 *
 * <p>Indicates the request requires user authentication. The client should
 * repeat the request with valid credentials. Unlike 403 Forbidden,
 * authentication may allow access.</p>
 *
 * <p>Usually thrown by authentication filters rather than application code.</p>
 *
 * @see HttpExceptionBase
 * @see ForbiddenException
 */
public class UnauthorizedException extends HttpExceptionBase {

    /**
     * Creates an UnauthorizedException with the given message.
     *
     * @param message the error description
     */
    public UnauthorizedException(String message) {
        super(HttpStatusCode.of(401), message);
    }
}