package net.magnesiumbackend.core.http;

import net.magnesiumbackend.core.route.RouteDefinition;
import net.magnesiumbackend.core.headers.HttpHeaderIndex;
import net.magnesiumbackend.core.headers.Slice;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public interface Request {

    String path();
    RouteDefinition routeDefinition();
    HttpMethod method();
    String body();
    HttpVersion version();
    Map<String, String> pathVariables();

    HttpHeaderIndex headerIndex();

    Map<String, String> queryParams();

    @Nullable Slice header(String name);

    default Map<String, String> cookies() {
        Slice cookieHeader = header("Cookie");

        if (cookieHeader == null || cookieHeader.length() == 0) {
            return Collections.emptyMap();
        }

        int i = 0;
        int end = cookieHeader.length();

        Map<String, String> cookies = new HashMap<>();

        while (i < end) {

            // skip spaces + semicolons
            while (i < end) {
                char c = cookieHeader.charAt(i);
                if (c != ' ' && c != ';') break;
                i++;
            }

            if (i >= end) break;

            int keyStart = i;

            while (i < end) {
                char c = cookieHeader.charAt(i);
                if (c == '=' || c == ';') break;
                i++;
            }

            int keyEnd = i;

            if (i >= end || cookieHeader.charAt(i) != '=') {
                while (i < end && cookieHeader.charAt(i) != ';') i++;
                continue;
            }

            // trim key
            while (keyStart < keyEnd && cookieHeader.charAt(keyStart) == ' ') keyStart++;
            while (keyEnd > keyStart && cookieHeader.charAt(keyEnd - 1) == ' ') keyEnd--;

            i++; // '='

            int valueStart = i;

            while (i < end && cookieHeader.charAt(i) != ';') {
                i++;
            }

            int valueEnd = i;

            // trim value
            while (valueStart < valueEnd && cookieHeader.charAt(valueStart) == ' ') valueStart++;
            while (valueEnd > valueStart && cookieHeader.charAt(valueEnd - 1) == ' ') valueEnd--;

            if (keyEnd > keyStart) {

                Slice keySlice = cookieHeader.slice(keyStart, keyEnd);
                Slice valueSlice = cookieHeader.slice(valueStart, valueEnd);

                String key = keySlice.materialize();
                String value = valueSlice.materialize();

                if (!key.isEmpty()) {
                    cookies.put(key, value);
                }
            }
        }

        return Collections.unmodifiableMap(cookies);
    }

    default @Nullable String queryParam(String name) {
        return queryParams().get(name);
    }
}