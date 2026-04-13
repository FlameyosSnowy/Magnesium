package net.magnesiumbackend.core.http.exceptions;

import net.magnesiumbackend.core.http.response.HttpStatusCode;

/**
 * Exception for 404 Not Found errors.
 *
 * <p>Indicates the requested resource could not be found on the server.
 * Commonly thrown when a database lookup returns no results.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * User user = userRepository.findById(id);
 * if (user == null) {
 *     throw new NotFoundException("User not found: " + id);
 * }
 * }</pre>
 *
 * @see HttpExceptionBase
 */
public class NotFoundException extends HttpExceptionBase {

    /**
     * Creates a NotFoundException with the given message.
     *
     * @param message the error description
     */
    public NotFoundException(String message) {
        super(HttpStatusCode.of(404), message);
    }
}