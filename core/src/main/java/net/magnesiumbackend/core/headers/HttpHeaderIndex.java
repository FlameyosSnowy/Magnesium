package net.magnesiumbackend.core.headers;

import java.util.HashMap;
import java.util.Map;

public final class HttpHeaderIndex {

    private final Slice[] headersById;
    private final Map<String, Slice> fallback;

    public HttpHeaderIndex(byte[] raw) {
        this.headersById = new Slice[HeaderRegistry.COUNT];
        this.fallback = new HashMap<>(8);
        parse(raw);
    }

    private void parse(byte[] raw) {

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