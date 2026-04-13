package net.magnesiumbackend.core.http.exceptions;

import net.magnesiumbackend.core.http.response.HttpStatusCode;

/**
 * Exception for 422 Unprocessable Content errors.
 *
 * <p>Indicates the server understands the content type and syntax of the request,
 * but was unable to process the contained instructions. Semantic errors, unlike
 * 400 Bad Request which indicates syntactic errors.</p>
 *
 * <p>Example use cases:</p>
 * <ul>
 *   <li>Missing required fields in a valid JSON payload</li>
 *   <li>Business rule violations (e.g., negative order quantity)</li>
 *   <li>Invalid state transitions</li>
 * </ul>
 *
 * @see HttpExceptionBase
 */
public class UnprocessableContentException extends HttpExceptionBase {

    /**
     * Creates an UnprocessableContentException with the given message.
     *
     * @param message the error description
     */
    public UnprocessableContentException(String message) {
        super(HttpStatusCode.of(422), message);
    }
}