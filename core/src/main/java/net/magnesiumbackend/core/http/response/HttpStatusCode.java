package net.magnesiumbackend.core.http.response;

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

    public static HttpStatusCode of(int code) {
        if (code < 100 || code > 599) {
            throw new IllegalArgumentException("Invalid HTTP status: " + code);
        }
        return CACHE[code];
    }

    public int value() {
        return code;
    }

    public boolean isKnown() {
        return known != null;
    }

    public HttpStatus known() {
        return known;
    }

    public Series series() {
        return Series.from(code);
    }

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