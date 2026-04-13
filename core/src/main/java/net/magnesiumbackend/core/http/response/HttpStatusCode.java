package net.magnesiumbackend.core.http.response;

/**
 * Represents an HTTP status code with pre-allocated cached instances.
 *
 * <p>Provides flyweight pattern caching for all valid HTTP status codes (100-599),
 * eliminating object allocation for common status codes. Each instance carries
 * both the numeric code and the corresponding {@link HttpStatus} enum if known.</p>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * HttpStatusCode ok = HttpStatusCode.of(200);
 * HttpStatusCode notFound = HttpStatusCode.of(404);
 *
 * // Check status properties
 * if (code.isError()) { ... }
 * if (code.series() == Series.CLIENT_ERROR) { ... }
 *
 * // Get known enum if available
 * code.known().ifPresent(status -> ...);
 * }</pre>
 *
 * @see HttpStatus
 * @see Series
 */
public final class HttpStatusCode {

    private static final HttpStatusCode[] CACHE = new HttpStatusCode[600];

    static {
        for (int i = 100; i <= 599; i++) {
            CACHE[i] = new HttpStatusCode(i, HttpStatus.resolve(i));
        }
    }

    private final int code;
    private final HttpStatus known;

    private HttpStatusCode(int code, HttpStatus known) {
        this.code = code;
        this.known = known;
    }

    /**
     * Returns a cached HttpStatusCode for the given status code.
     *
     * @param code the HTTP status code (100-599)
     * @return the cached HttpStatusCode instance
     * @throws IllegalArgumentException if code is outside 100-599 range
     */
    public static HttpStatusCode of(int code) {
        if (code < 100 || code > 599) {
            throw new IllegalArgumentException("Invalid HTTP status: " + code);
        }
        return CACHE[code];
    }

    /**
     * Returns the numeric status code value.
     *
     * @return the HTTP status code (e.g., 200, 404)
     */
    public int value() {
        return code;
    }

    /**
     * Returns true if this status code has a known {@link HttpStatus} enum value.
     *
     * @return true if the status code is defined in {@link HttpStatus}
     */
    public boolean isKnown() {
        return known != null;
    }

    /**
     * Returns the known {@link HttpStatus} enum value, or null if unknown.
     *
     * @return the HttpStatus enum, or null
     */
    public HttpStatus known() {
        return known;
    }

    /**
     * Returns the {@link Series} (category) of this status code.
     *
     * @return the status series (INFORMATIONAL, SUCCESS, REDIRECTION, CLIENT_ERROR, SERVER_ERROR)
     */
    public Series series() {
        return Series.from(code);
    }

    /**
     * Returns true if this is an error status code (4xx or 5xx).
     *
     * @return true if code >= 400
     */
    public boolean isError() {
        return code >= 400;
    }

    @Override
    public String toString() {
        return known != null
                ? known.toString()
                : code + " (Unknown)";
    }
}