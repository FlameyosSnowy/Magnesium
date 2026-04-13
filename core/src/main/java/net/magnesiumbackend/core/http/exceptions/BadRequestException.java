package net.magnesiumbackend.core.http.exceptions;

import net.magnesiumbackend.core.http.response.HttpStatusCode;

/**
 * Exception for 400 Bad Request errors.
 *
 * <p>Indicates the server cannot or will not process the request due to
 * client error (malformed request syntax, invalid message framing, etc.).</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * if (invalidInput) {
 *     throw new BadRequestException("Invalid input: name is required");
 * }
 * }</pre>
 *
 * @see HttpExceptionBase
 */
public class BadRequestException extends HttpExceptionBase {

    /**
     * Creates a BadRequestException with the given message.
     *
     * @param message the error description
     */
    public BadRequestException(String message) {
        super(HttpStatusCode.of(400), message);
    }

    /**
     * Creates a BadRequestException with message and cause.
     *
     * @param message the error description
     * @param cause   the underlying cause
     */
    public BadRequestException(String message, Throwable cause) {
        super(HttpStatusCode.of(400), message, cause);
    }
}