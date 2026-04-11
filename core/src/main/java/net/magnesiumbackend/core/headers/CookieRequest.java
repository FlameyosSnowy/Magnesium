package net.magnesiumbackend.core.headers;

import java.util.Map;

public final class CookieRequest {

    private final HttpHeaderIndex headers;

    public CookieRequest(HttpHeaderIndex headers) {
        this.headers = headers;
    }

    public Slice header(String name) {
        return headers.get(name);
    }

    public Map<String, String> cookies() {
        Slice cookie = headers.get("cookie");
        return CookieParser.parse(cookie);
    }
}