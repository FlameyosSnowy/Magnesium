package net.magnesiumbackend.core.security.exceptions;

import net.magnesiumbackend.core.http.response.ErrorResponse;
import net.magnesiumbackend.core.http.response.HttpStatus;
import net.magnesiumbackend.core.http.response.ResponseEntity;

/**
 * Exception thrown when CSRF token validation fails.
 *
 * <p>This exception indicates a potential cross-site request forgery attack
 * or a misconfigured client that failed to include or match the required
 * CSRF token.</p>
 *
 * <p>Returns HTTP 403 Forbidden status.</p>
 *
 * @see net.magnesiumbackend.core.security.CsrfFilter
 */
public class CsrfException extends SecurityException {

    /**
     * Creates a CSRF exception with default message.
     */
    public CsrfException() {
        this("Invalid or missing CSRF token");
    }

    /**
     * Creates a CSRF exception with custom message.
     *
     * @param message the error message
     */
    public CsrfException(String message) {
        super(message);
    }

    /**
     * Creates a CSRF exception with message and cause.
     *
     * @param message the error message
     * @param cause the underlying cause
     */
    public CsrfException(String message, Throwable cause) {
        super(message, cause);
    }
}
