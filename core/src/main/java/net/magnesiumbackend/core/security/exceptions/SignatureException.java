package net.magnesiumbackend.core.security.exceptions;

/**
 * Exception thrown when HMAC signature verification fails.
 *
 * <p>Indicates that the request signature could not be verified, which may mean:
 * <ul>
 *   <li>Missing signature or timestamp headers</li>
 *   <li>Timestamp outside acceptable window (replay attack)</li>
 *   <li>Signature mismatch (tampered request)</li>
 * </ul>
 * </p>
 *
 * @see net.magnesiumbackend.core.security.RequestSigningFilter
 */
public final class SignatureException extends RuntimeException {
    /**
     * Creates a new SignatureException with the given message.
     *
     * @param message the error description
     */
    public SignatureException(String message) { super(message); }
}