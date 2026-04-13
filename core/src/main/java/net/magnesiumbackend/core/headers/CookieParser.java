package net.magnesiumbackend.core.headers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple cookie header parser that materializes values immediately.
 *
 * <p>Unlike {@link CookieIndex} which keeps values as slices, this parser
 * converts all values to Strings immediately. Use this when you need all
 * cookies as Strings and don't need the lazy evaluation benefits of Slice.</p>
 *
 * <h3>Format</h3>
 * <pre>name1=value1; name2=value2; name3=value3</pre>
 *
 * @see CookieIndex
 * @see Slice
 */
public final class CookieParser {

    /**
     * Parses a Cookie header value into a map of name to value.
     *
     * @param cookieHeader the Cookie header value (may be null or empty)
     * @return an unmodifiable map of cookie names to values
     */
    public static Map<String, String> parse(Slice cookieHeader) {
        if (cookieHeader == null || cookieHeader.length() == 0) {
            return Collections.emptyMap();
        }

        int i = 0;
        int end = cookieHeader.length();

        Map<String, String> map = new HashMap<>(8);

        while (i < end) {

            while (i < end) {
                char c = cookieHeader.charAt(i);
                if (c != ' ' && c != ';') break;
                i++;
            }

            if (i >= end) break;

            int keyStart = i;

            // find '=' or ';'
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

            while (keyStart < keyEnd && isSpace(cookieHeader.charAt(keyStart))) keyStart++;
            while (keyEnd > keyStart && isSpace(cookieHeader.charAt(keyEnd - 1))) keyEnd--;

            i++;

            int valStart = i;

            while (i < end && cookieHeader.charAt(i) != ';') {
                i++;
            }

            int valEnd = i;

            while (valStart < valEnd && isSpace(cookieHeader.charAt(valStart))) valStart++;
            while (valEnd > valStart && isSpace(cookieHeader.charAt(valEnd - 1))) valEnd--;

            if (keyEnd > keyStart) {
                String key = cookieHeader.slice(keyStart, keyEnd).materialize();
                String val = cookieHeader.slice(valStart, valEnd).materialize();
                map.put(key, val);
            }
        }

        return Collections.unmodifiableMap(map);
    }

    private static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }
}