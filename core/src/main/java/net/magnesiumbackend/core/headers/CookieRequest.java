package net.magnesiumbackend.core.headers;

import java.util.Map;

public final class CookieRequest {

    private final HttpHeaderIndex headers;
    private CookieIndex cookieIndex;

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