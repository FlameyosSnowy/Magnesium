package net.magnesiumbackend.core.http.exceptions;

import net.magnesiumbackend.core.http.response.HttpStatusCode;

/**
 * Exception for 500 Internal Server Error.
 *
 * <p>Indicates an unexpected condition on the server that prevented it from
 * fulfilling the request. Used when no other specific exception applies.</p>
 *
 * <p>Usually the framework handles unexpected exceptions automatically, but
 * this can be thrown explicitly for known server-side failures.</p>
 *
 * @see HttpExceptionBase
 */
public class InternalServerErrorException extends HttpExceptionBase {

    /**
     * Creates an InternalServerErrorException with the given message.
     *
     * @param message the error description
     */
    public InternalServerErrorException(String message) {
        super(HttpStatusCode.of(500), message);
    }

    /**
     * Creates an InternalServerErrorException with message and cause.
     *
     * @param message the error description
     * @param cause   the underlying exception
     */
    public InternalServerErrorException(String message, Throwable cause) {
        super(HttpStatusCode.of(500), message, cause);
    }
}