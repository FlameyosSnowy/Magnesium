package net.magnesiumbackend.core.headers;

import java.util.HashMap;
import java.util.Map;

/**
 * Fast index for Cookie header parsing with lazy evaluation.
 *
 * <p>CookieIndex parses the Cookie header on-demand, extracting name-value pairs
 * into an index for O(1) lookups. Known cookies (session, csrf, userId) use
 * a fast array lookup, while dynamic cookies use a HashMap fallback.</p>
 *
 * <h3>Format</h3>
 * <pre>name1=value1; name2=value2; name3=value3</pre>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * Slice cookieHeader = headers.get("cookie");
 * CookieIndex cookies = new CookieIndex(cookieHeader);
 *
 * // Access cookie values
 * Slice sessionId = cookies.get("session");
 * if (sessionId != null) {
 *     String value = sessionId.materialize();
 * }
 * }</pre>
 *
 * @see CookieRegistry
 * @see CookieParser
 */
public final class CookieIndex {

    private final Slice raw;

    // fast-path for known cookies (optional registry-based like headers)
    private final Slice[] cookiesById;

    // fallback for dynamic cookies
    private final Map<String, Slice> fallback;

    private boolean parsed;

    /**
     * Creates a new cookie index from the raw cookie header value.
     *
     * @param raw the Cookie header value (may be null)
     */
    public CookieIndex(Slice raw) {
        this.raw = raw;
        this.cookiesById = new Slice[CookieRegistry.COUNT]; // optional optimization
        this.fallback = new HashMap<>(4);
    }

    public void parseIfNeeded() {
        if (parsed) return;
        parsed = true;

        if (raw == null || raw.length() == 0) return;

        int i = 0;
        int end = raw.length();

        while (i < end) {

            while (i < end) {
                char c = raw.charAt(i);
                if (c != ' ' && c != ';') break;
                i++;
            }

            if (i >= end) break;

            int keyStart = i;

            while (i < end) {
                char c = raw.charAt(i);
                if (c == '=' || c == ';') break;
                i++;
            }

            int keyEnd = i;

            if (i >= end || raw.charAt(i) != '=') {
                while (i < end && raw.charAt(i) != ';') i++;
                continue;
            }

            while (keyStart < keyEnd && isSpace(raw.charAt(keyStart))) keyStart++;
            while (keyEnd > keyStart && isSpace(raw.charAt(keyEnd - 1))) keyEnd--;

            i++; // skip '='

            int valStart = i;

            while (i < end && raw.charAt(i) != ';') i++;

            int valEnd = i;

            while (valStart < valEnd && isSpace(raw.charAt(valStart))) valStart++;
            while (valEnd > valStart && isSpace(raw.charAt(valEnd - 1))) valEnd--;

            if (keyEnd <= keyStart) continue;

            Slice keySlice = raw.slice(keyStart, keyEnd);
            Slice valueSlice = raw.slice(valStart, valEnd);

            int id = CookieRegistry.resolve(keySlice);

            if (id >= 0) {
                cookiesById[id] = valueSlice;
            } else {
                fallback.put(keySlice.materializeLowercase(), valueSlice);
            }
        }
    }

    public Slice get(String name) {
        int id = CookieRegistry.resolve(name);
        if (id >= 0) return cookiesById[id];

        return fallback.get(name.toLowerCase());
    }

    public String getValue(String name) {
        Slice s = get(name);
        return s != null ? s.materialize() : null;
    }

    public boolean contains(String name) {
        return get(name) != null;
    }

    private static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }
}