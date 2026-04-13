package net.magnesiumbackend.core.http.response;

/**
 * Categorizes HTTP status codes into series (1xx, 2xx, 3xx, 4xx, 5xx).
 *
 * <p>Each series represents a class of HTTP response:</p>
 * <ul>
 *   <li>{@link #INFORMATIONAL} - 1xx: Request received, continuing process</li>
 *   <li>{@link #SUCCESS} - 2xx: Request successfully received, understood, and accepted</li>
 *   <li>{@link #REDIRECTION} - 3xx: Further action must be taken to complete the request</li>
 *   <li>{@link #CLIENT_ERROR} - 4xx: Request contains bad syntax or cannot be fulfilled</li>
 *   <li>{@link #SERVER_ERROR} - 5xx: Server failed to fulfill an apparently valid request</li>
 * </ul>
 *
 * @see HttpStatusCode
 * @see HttpStatus
 */
public enum Series {
    /** 1xx status codes - Informational responses. */
    INFORMATIONAL,

    /** 2xx status codes - Successful responses. */
    SUCCESS,

    /** 3xx status codes - Redirection messages. */
    REDIRECTION,

    /** 4xx status codes - Client error responses. */
    CLIENT_ERROR,

    /** 5xx status codes - Server error responses. */
    SERVER_ERROR;

    /**
     * Determines the series for a given HTTP status code.
     *
     * @param code the HTTP status code
     * @return the corresponding series
     * @throws IllegalArgumentException if code is not a valid HTTP status
     */
    public static Series from(int code) {
        return switch (code / 100) {
            case 1 -> INFORMATIONAL;
            case 2 -> SUCCESS;
            case 3 -> REDIRECTION;
            case 4 -> CLIENT_ERROR;
            case 5 -> SERVER_ERROR;
            default -> throw new IllegalArgumentException("Invalid HTTP status: " + code);
        };
    }
}