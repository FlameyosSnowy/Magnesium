package net.magnesiumbackend.core.headers;

import java.util.Map;

/**
 * Convenience wrapper for accessing cookies from HTTP headers.
 *
 * <p>Combines header access and lazy cookie parsing into a single interface.
 * Cookie parsing only occurs when {@link #cookies()} is first called.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * HttpHeaderIndex headers = ...;
 * CookieRequest request = new CookieRequest(headers);
 *
 * // Access headers
 * Slice auth = request.header("Authorization");
 *
 * // Access cookies (lazy parsing)
 * CookieIndex cookies = request.cookies();
 * Slice session = cookies.get("session");
 * }</pre>
 *
 * @see HttpHeaderIndex
 * @see CookieIndex
 */
public final class CookieRequest {

    private final HttpHeaderIndex headers;
    private CookieIndex cookieIndex;

    /**
     * Creates a new cookie request wrapper.
     *
     * @param headers the HTTP headers to wrap
     */
    public CookieRequest(HttpHeaderIndex headers) {
        this.headers = headers;
    }

    public Slice header(String name) {
        return headers.get(name);
    }

    public CookieIndex cookies() {
        if (cookieIndex == null) {
            cookieIndex = new CookieIndex(headers.get("cookie"));
        }
        return cookieIndex;
    }
}