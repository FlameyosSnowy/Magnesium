package net.magnesiumbackend.core.http.response;

public enum HttpStatus {

    // --- 1xx ---
    CONTINUE(100, "Continue"),
    SWITCHING_PROTOCOLS(101, "Switching Protocols"),
    EARLY_HINTS(103, "Early Hints"),

    // --- 2xx ---
    OK(200, "OK"),
    CREATED(201, "Created"),
    ACCEPTED(202, "Accepted"),
    NO_CONTENT(204, "No Content"),
    PARTIAL_CONTENT(206, "Partial Content"),

    // --- 3xx ---
    MOVED_PERMANENTLY(301, "Moved Permanently"),
    FOUND(302, "Found"),
    SEE_OTHER(303, "See Other"),
    NOT_MODIFIED(304, "Not Modified"),
    TEMPORARY_REDIRECT(307, "Temporary Redirect"),
    PERMANENT_REDIRECT(308, "Permanent Redirect"),

    // --- 4xx ---
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
    CONFLICT(409, "Conflict"),
    GONE(410, "Gone"),
    PAYLOAD_TOO_LARGE(413, "Payload Too Large"),
    UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),
    UNPROCESSABLE_CONTENT(422, "Unprocessable Content"),
    TOO_MANY_REQUESTS(429, "Too Many Requests"),

    // --- 5xx ---
    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    NOT_IMPLEMENTED(501, "Not Implemented"),
    BAD_GATEWAY(502, "Bad Gateway"),
    SERVICE_UNAVAILABLE(503, "Service Unavailable"),
    GATEWAY_TIMEOUT(504, "Gateway Timeout");

    private final int code;
    private final String reason;

    private static final java.util.Map<Integer, HttpStatus> LOOKUP = new java.util.HashMap<>();

    static {
        for (HttpStatus s : values()) {
            LOOKUP.put(s.code, s);
        }
    }

    HttpStatus(int code, String reason) {
        this.code = code;
        this.reason = reason;
    }

    public int code() {
        return code;
    }

    public String reason() {
        return reason;
    }

    public Series series() {
        return Series.from(code);
    }

    public boolean isError() {
        return code >= 400;
    }

    public static HttpStatus resolve(int code) {
        return LOOKUP.get(code);
    }

    public static HttpStatus require(int code) {
        HttpStatus status = LOOKUP.get(code);
        if (status == null) {
            throw new IllegalArgumentException("Unknown HTTP status: " + code);
        }
        return status;
    }

    @Override
    public String toString() {
        return code + " " + reason;
    }
}