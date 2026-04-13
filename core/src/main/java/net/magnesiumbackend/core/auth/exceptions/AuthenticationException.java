package net.magnesiumbackend.core.auth.exceptions;

/**
 * Exception thrown when authentication fails.
 *
 * <p>AuthenticationExceptions signal that credentials were present but invalid
 * (expired token, wrong password, invalid signature, etc.). This is distinct from
 * the case where no credentials were provided at all.</p>
 *
 * <p>When thrown by an {@link net.magnesiumbackend.core.auth.AuthenticationProvider},
 * the framework returns HTTP 401 Unauthorized to the client.</p>
 *
 * <p>Example usage in a custom provider:</p>
 * <pre>{@code
 * public Optional<Principal> authenticate(RequestContext ctx) {
 *     String token = extractToken(ctx);
 *     if (token != null && !isValid(token)) {
 *         throw new AuthenticationException("Token has expired");
 *     }
 *     // ...
 * }
 * }</pre>
 *
 * @see net.magnesiumbackend.core.auth.AuthenticationProvider
 * @see net.magnesiumbackend.core.auth.AuthenticationFilter
 */
public class AuthenticationException extends RuntimeException {
    /**
     * Creates a new authentication exception with the given message.
     *
     * @param message the error message (may be shown to client)
     */
    public AuthenticationException(String message) {
        super(message);
    }

    /**
     * Creates a new authentication exception with message and cause.
     *
     * @param message the error message
     * @param cause   the underlying exception that caused the failure
     */
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
