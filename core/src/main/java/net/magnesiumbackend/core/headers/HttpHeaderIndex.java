package net.magnesiumbackend.core.headers;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fast index for HTTP headers with O(1) lookup for known headers.
 *
 * <p>Parses the raw HTTP header block into an index that supports both fast
 * array-based lookups for known headers ({@link HeaderRegistry}) and HashMap
 * fallback for custom headers. The parsing is done once during construction.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * byte[] headerBlock = ...; // Raw HTTP headers
 * HttpHeaderIndex headers = new HttpHeaderIndex(headerBlock);
 *
 * // Fast lookup for known headers
 * Slice contentType = headers.get(HeaderRegistry.CONTENT_TYPE);
 *
 * // Generic lookup by name
 * Slice auth = headers.get("Authorization");
 * }</pre>
 *
 * @see HeaderRegistry
 * @see HeaderResolver
 * @see CookieRequest
 */
public final class HttpHeaderIndex {
    private static final HttpHeaderIndex EMPTY_INDEX = new HttpHeaderIndex(new byte[0]);

    private final Slice[] headersById;
    private final Map<String, Slice> fallback;

    /**
     * Creates a new header index from the raw header block.
     *
     * @param raw the raw HTTP header bytes
     */
    public HttpHeaderIndex(byte[] raw) {
        if (raw == null || raw.length == 0) {
            this.headersById = new Slice[0];
            this.fallback = new HashMap<>(0);
            return;
        }
        this.headersById = new Slice[HeaderRegistry.COUNT];
        this.fallback = new HashMap<>(8);
        parse(raw);
    }

    public HttpHeaderIndex(Map<String, List<String>> requestHeaders) {
        this.headersById = new Slice[HeaderRegistry.COUNT];
        this.fallback = new HashMap<>(8);

        for (Map.Entry<String, List<String>> entry : requestHeaders.entrySet()) {

            String name = entry.getKey();
            List<String> values = entry.getValue();

            if (values == null || values.isEmpty()) continue;

            String value = values.getFirst();

            Slice valueSlice = new Slice(
                value.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                0,
                value.length()
            );

            int id = HeaderResolver.resolveString(name);

            if (id >= 0) {
                headersById[id] = valueSlice;
            } else {
                fallback.put(name.toLowerCase(), valueSlice);
            }
        }
    }

    public static HttpHeaderIndex empty() {
        return EMPTY_INDEX;
    }

    private void parse(byte @NotNull [] raw) {

        int i = 0;
        int len = raw.length;

        while (i < len) {

            int nameStart = i;

            while (i < len && raw[i] != ':') i++;
            if (i >= len) break;

            int nameEnd = i;

            do {
                i++;
            } while (i < len && raw[i] == ' ');

            int valueStart = i;

            while (i < len && raw[i] != '\r' && raw[i] != '\n') i++;

            int valueEnd = i;

            Slice nameSlice = new Slice(raw, nameStart, nameEnd - nameStart);
            Slice valueSlice = new Slice(raw, valueStart, valueEnd - valueStart);

            int id = HeaderResolver.resolve(nameSlice);

            if (id >= 0) {
                headersById[id] = valueSlice;
            } else {
                fallback.put(nameSlice.materializeLowercase(), valueSlice);
            }

            while (i < len && (raw[i] == '\r' || raw[i] == '\n')) i++;
        }
    }

    public Slice get(int headerId) {
        return headersById[headerId];
    }

    public Slice get(String name) {
        int id = HeaderResolver.resolveString(name);
        if (id >= 0) return headersById[id];
        return fallback.get(name.toLowerCase());
    }
}