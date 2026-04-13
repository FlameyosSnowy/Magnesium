package net.magnesiumbackend.core.http.exceptions;

import net.magnesiumbackend.core.http.response.HttpStatusCode;

/**
 * Base class for HTTP exceptions with status code and message.
 *
 * <p>HttpExceptionBase provides a standard implementation of {@link HttpException}
 * that carries an HTTP status code and optional message/cause. All standard
 * HTTP exceptions in the framework extend this class.</p>
 *
 * <h3>Creating Custom Exceptions</h3>
 * <pre>{@code
 * public class PaymentRequiredException extends HttpExceptionBase {
 *     public PaymentRequiredException(String message) {
 *         super(HttpStatusCode.of(402), message);
 *     }
 * }
 * }</pre>
 *
 * @see HttpException
 * @see HttpStatusCode
 */
public class HttpExceptionBase extends RuntimeException implements HttpException {

    private final HttpStatusCode status;

    /**
     * Creates a new HTTP exception with the given status and message.
     *
     * @param status  the HTTP status code
     * @param message the error message
     */
    public HttpExceptionBase(HttpStatusCode status, String message) {
        super(message);
        this.status = status;
    }

    /**
     * Creates a new HTTP exception with status, message, and cause.
     *
     * @param status  the HTTP status code
     * @param message the error message
     * @param cause   the underlying cause
     */
    public HttpExceptionBase(HttpStatusCode status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    @Override
    public HttpStatusCode status() {
        return status;
    }
}