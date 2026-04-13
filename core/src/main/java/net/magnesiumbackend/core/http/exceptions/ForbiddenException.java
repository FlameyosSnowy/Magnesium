package net.magnesiumbackend.core.http.exceptions;

import net.magnesiumbackend.core.http.response.HttpStatusCode;

/**
 * Exception for 403 Forbidden errors.
 *
 * <p>Indicates the server understood the request but refuses to authorize it.
 * Unlike 401 Unauthorized, the client is authenticated but lacks permission
 * to access the resource.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * if (!principal.hasPermission("admin")) {
 *     throw new ForbiddenException("Admin access required");
 * }
 * }</pre>
 *
 * @see HttpExceptionBase
 * @see UnauthorizedException
 */
public class ForbiddenException extends HttpExceptionBase {

    /**
     * Creates a ForbiddenException with the given message.
     *
     * @param message the error description
     */
    public ForbiddenException(String message) {
        super(HttpStatusCode.of(403), message);
    }
}