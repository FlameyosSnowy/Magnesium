package net.magnesiumbackend.core.http;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class HttpUtils {
    private HttpUtils() {}

    public static Map<String, String> parseQueryString(String query) {
        if (query == null || query.isEmpty()) return Map.of();

        Map<String, String> params = new HashMap<>(8);
        int len = query.length();
        int start = 0;

        while (start < len) {
            // find '=' within this pair
            int eq = -1;
            int end = start;
            while (end < len) {
                char c = query.charAt(end);
                if (c == '=' && eq == -1) eq = end;
                else if (c == '&') break;
                end++;
            }

            if (eq > start) { // ignore malformed pairs with no key
                String key   = URLDecoder.decode(query.substring(start, eq),  StandardCharsets.UTF_8);
                String value = URLDecoder.decode(query.substring(eq + 1, end), StandardCharsets.UTF_8);
                params.putIfAbsent(key, value); // first value wins for duplicate keys
            }

            start = end + 1;
        }

        return params;
    }
}