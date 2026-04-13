package net.magnesiumbackend.core.http.exceptions;

import net.magnesiumbackend.core.http.response.HttpStatusCode;

/**
 * Exception for 503 Service Unavailable errors.
 *
 * <p>Indicates the server is currently unable to handle the request due to
 * temporary overload or maintenance. Often used with circuit breakers when
 * downstream services are unavailable.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * try {
 *     return breaker.execute(() -> externalService.call());
 * } catch (CircuitOpenException e) {
 *     throw new ServiceUnavailableException("Payment service temporarily unavailable");
 * }
 * }</pre>
 *
 * @see HttpExceptionBase
 * @see net.magnesiumbackend.core.circuit.CircuitOpenException
 */
public class ServiceUnavailableException extends HttpExceptionBase {

    /**
     * Creates a ServiceUnavailableException with the given message.
     *
     * @param message the error description
     */
    public ServiceUnavailableException(String message) {
        super(HttpStatusCode.of(503), message);
    }
}