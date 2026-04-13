package net.magnesiumbackend.core.auth.exceptions;

import net.magnesiumbackend.core.auth.jwt.NimbusJwtVerifier;

/**
 * Exception thrown when JWT verification fails.
 *
 * <p>Specialized exception for JWT-specific errors such as:
 * <ul>
 *   <li>Invalid signature</li>
 *   <li>Expired token</li>
 *   <li>Malformed JWT structure</li>
 *   <li>Unsupported algorithm</li>
 * </ul>
 * </p>
 *
 * <p>This exception is typically wrapped in an {@link AuthenticationException}
 * by the authentication provider before propagating to the filter chain.</p>
 *
 * @see NimbusJwtVerifier
 * @see AuthenticationException
 */
public class JwtVerificationException extends RuntimeException {
    /**
     * Creates a new JWT verification exception.
     *
     * @param message the error description
     */
    public JwtVerificationException(String message) {
        super(message);
    }
}
