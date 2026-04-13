package net.magnesiumbackend.core.http.exceptions;

import net.magnesiumbackend.core.http.response.HttpStatusCode;

/**
 * Exception for 429 Too Many Requests errors.
 *
 * <p>Indicates the user has sent too many requests in a given amount of time
 * (rate limiting). Usually thrown by rate limit filters rather than application
 * code.</p>
 *
 * <p>The response may include a Retry-After header indicating when to retry.</p>
 *
 * @see HttpExceptionBase
 * @see net.magnesiumbackend.core.annotations.RateLimit
 */
public class TooManyRequestsException extends HttpExceptionBase {

    /**
     * Creates a TooManyRequestsException with the given message.
     *
     * @param message the error description
     */
    public TooManyRequestsException(String message) {
        super(HttpStatusCode.of(429), message);
    }
}