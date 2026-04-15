package net.magnesiumbackend.core.http.response;

import net.magnesiumbackend.core.headers.HttpQueryParamIndex;

import java.nio.charset.StandardCharsets;

public final class HttpUtils {
    private HttpUtils() {}

    public static HttpQueryParamIndex parseQueryString(String query) {
        if (query == null || query.isEmpty()) {
            return HttpQueryParamIndex.empty();
        }

        byte[] bytes = query.getBytes(StandardCharsets.UTF_8);
        return new HttpQueryParamIndex(bytes);
    }

    public static HttpQueryParamIndex parseQueryString(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return HttpQueryParamIndex.empty();
        }

        return new HttpQueryParamIndex(bytes);
    }


}