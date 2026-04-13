package net.magnesiumbackend.core.http.exceptions;

import net.magnesiumbackend.core.http.response.HttpStatusCode;

/**
 * Marker interface for exceptions that map to HTTP status codes.
 *
 * <p>Implementations of this interface can be thrown from controller methods
 * and will be automatically converted to HTTP responses with the appropriate
 * status code by the framework's exception handler.</p>
 *
 * <h3>Standard Exceptions</h3>
 * <ul>
 *   <li>{@link BadRequestException} - 400 Bad Request</li>
 *   <li>{@link UnauthorizedException} - 401 Unauthorized</li>
 *   <li>{@link ForbiddenException} - 403 Forbidden</li>
 *   <li>{@link NotFoundException} - 404 Not Found</li>
 *   <li>{@link ConflictException} - 409 Conflict</li>
 *   <li>{@link UnprocessableContentException} - 422 Unprocessable Content</li>
 *   <li>{@link TooManyRequestsException} - 429 Too Many Requests</li>
 *   <li>{@link InternalServerErrorException} - 500 Internal Server Error</li>
 *   <li>{@link ServiceUnavailableException} - 503 Service Unavailable</li>
 * </ul>
 *
 * @see HttpExceptionBase
 * @see net.magnesiumbackend.core.annotations.ExceptionHandler
 */
public interface HttpException {
    /**
     * Returns the HTTP status code for this exception.
     *
     * @return the status code to return to the client
     */
    HttpStatusCode status();
}