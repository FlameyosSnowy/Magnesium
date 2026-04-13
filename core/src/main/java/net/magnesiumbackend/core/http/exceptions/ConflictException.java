package net.magnesiumbackend.core.http.exceptions;

import net.magnesiumbackend.core.http.response.HttpStatusCode;

/**
 * Exception for 409 Conflict errors.
 *
 * <p>Indicates the request could not be completed due to a conflict with the
 * current state of the resource. Commonly used for optimistic locking failures
 * or duplicate resource creation attempts.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * if (version != currentVersion) {
 *     throw new ConflictException("Resource was modified by another user");
 * }
 * }</pre>
 *
 * @see HttpExceptionBase
 */
public class ConflictException extends HttpExceptionBase {

    /**
     * Creates a ConflictException with the given message.
     *
     * @param message the error description
     */
    public ConflictException(String message) {
        super(HttpStatusCode.of(409), message);
    }
}