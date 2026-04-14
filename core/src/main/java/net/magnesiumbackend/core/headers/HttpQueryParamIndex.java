package net.magnesiumbackend.core.headers;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class HttpQueryParamIndex {
    private static final byte[] EMPTY = new byte[0];
    private static final byte[][] EMPTY_KEYS = new byte[0][];
    private static final int[] EMPTY_INT_ARRAY = new int[0];
    private static final Slice[][] EMPTY_VALUES = new Slice[0][2];
    private static final String[][] EMPTY_DECODED_CACHE = new String[0][2];

    private byte[][] keys;
    private int[] hashes;

    private Slice[][] values;
    private int[] counts;

    private String[][] decodedCache;

    private int size;
    private int mask;

    private static final float LOAD_FACTOR = 0.75f;

    private final byte[] raw;

    public HttpQueryParamIndex(byte[] raw) {
        if (raw == null) {
            this.raw = EMPTY;
            init(0);
            return;
        }
        this.raw = raw;

        int capacity = 16;
        init(capacity);

        parse();
    }

    private void init(int capacity) {
        if (capacity < 0) {
            keys = EMPTY_KEYS;
            hashes = EMPTY_INT_ARRAY;
            values = EMPTY_VALUES;
            counts = EMPTY_INT_ARRAY;
            decodedCache = EMPTY_DECODED_CACHE;
            return;
        }

        keys = new byte[capacity][];
        hashes = new int[capacity];

        values = new Slice[capacity][2];
        counts = new int[capacity];

        decodedCache = new String[capacity][2];

        mask = capacity - 1;
    }

    private void resize() {
        byte[][] oldKeys = keys;
        int[] oldHashes = hashes;
        Slice[][] oldValues = values;
        int[] oldCounts = counts;

        init(keys.length << 1);
        size = 0;

        int length = oldKeys.length;
        for (int i = 0; i < length; i++) {
            byte[] k = oldKeys[i];
            if (k == null) continue;

            for (int j = 0; j < oldCounts[i]; j++) {
                putInternal(k, oldHashes[i], oldValues[i][j]);
            }
        }
    }

    private boolean needsResize() {
        return size + 1 > keys.length * LOAD_FACTOR;
    }

    private void parse() {
        int i = 0;

        byte[] raw = this.raw;
        int length = raw.length;
        while (i < length) {
            int keyStart = i;
            int keyEnd = -1;

            while (i < length) {
                byte c = raw[i];
                if (c == '=' && keyEnd == -1) {
                    keyEnd = i;
                } else if (c == '&') {
                    break;
                }
                i++;
            }

            int end = i;

            if (keyEnd > keyStart) {
                byte[] key = Arrays.copyOfRange(raw, keyStart, keyEnd);
                Slice value = new Slice(raw, keyEnd + 1, end - keyEnd - 1);

                int h = hash(key);
                putInternal(key, h, value);
            }

            i++;
        }
    }

    private static int hash(byte[] key) {
        int h = 1;
        for (byte b : key) {
            h = 31 * h + (b & 0xFF);
        }
        return h;
    }

    private void putInternal(byte[] key, int hash, Slice value) {
        if (needsResize()) {
            resize();
        }

        int idx = hash & mask;

        while (true) {
            byte[] existing = keys[idx];

            if (existing == null) {
                keys[idx] = key;
                hashes[idx] = hash;

                values[idx][0] = value;
                counts[idx] = 1;

                size++;
                return;
            }

            if (hashes[idx] == hash && equals(existing, key)) {
                append(idx, value);
                return;
            }

            idx = (idx + 1) & mask;
        }
    }

    private void append(int idx, Slice value) {
        int c = counts[idx];

        if (c == values[idx].length) {
            values[idx] = Arrays.copyOf(values[idx], c * 2);
            decodedCache[idx] = Arrays.copyOf(decodedCache[idx], c * 2);
        }

        values[idx][c] = value;
        counts[idx]++;
    }

    public Slice getSlice(byte[] key) {
        int idx = find(key);
        if (idx < 0) return null;
        return values[idx][0];
    }

    public String get(byte[] key) {
        int idx = find(key);
        if (idx < 0) return null;

        if (decodedCache[idx][0] != null) {
            return decodedCache[idx][0];
        }

        return decodedCache[idx][0] = decode(values[idx][0]);
    }

    public String get(byte[] key, int index) {
        int idx = find(key);
        if (idx < 0 || index >= counts[idx]) return null;

        if (decodedCache[idx][index] != null) {
            return decodedCache[idx][index];
        }

        return decodedCache[idx][index] =
            decode(values[idx][index]);
    }

    public int size(byte[] key) {
        int idx = find(key);
        return idx < 0 ? 0 : counts[idx];
    }

    private int find(byte[] key) {
        int h = hash(key);
        int idx = h & mask;

        byte[][] keys = this.keys;
        while (true) {
            byte[] k = keys[idx];

            if (k == null) return -1;

            if (hashes[idx] == h && equals(k, key)) {
                return idx;
            }

            idx = (idx + 1) & mask;
        }
    }

    // ----------------------------
    // Byte compare (critical path)
    // ----------------------------

    private static boolean equals(byte[] a, byte[] b) {
        int length = a.length;
        if (length != b.length) return false;

        for (int i = 0; i < length; i++) {
            if (a[i] != b[i]) return false;
        }
        return true;
    }

    private String decode(Slice slice) {
        return new String(
            slice.src(),
            slice.start(),
            slice.len(),
            StandardCharsets.UTF_8
        );
    }
    public Slice getSlice(String key) {
        return getSlice(key.getBytes(StandardCharsets.UTF_8));
    }

    public String get(String key) {
        return get(key.getBytes(StandardCharsets.UTF_8));
    }
}