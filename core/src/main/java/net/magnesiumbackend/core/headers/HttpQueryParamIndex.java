package net.magnesiumbackend.core.headers;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class HttpQueryParamIndex {

    private static final byte[] EMPTY = new byte[0];
    private static final HttpQueryParamIndex EMPTY_INDEX = new HttpQueryParamIndex(EMPTY);

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
        if (raw == null || raw.length == 0) {
            this.raw = EMPTY;
            init(16);
            return;
        }

        this.raw = raw;
        init(16);
        parse();
    }

    public static HttpQueryParamIndex empty() {
        return EMPTY_INDEX;
    }

    private void init(int capacity) {
        keys = new byte[capacity][];
        hashes = new int[capacity];

        values = new Slice[capacity][2];
        counts = new int[capacity];

        decodedCache = new String[capacity][2];

        mask = capacity - 1;
    }

    private boolean needsResize() {
        return size + 1 > keys.length * LOAD_FACTOR;
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

    private void parse() {
        int i = 0;
        int len = raw.length;

        while (i < len) {
            int keyStart = i;
            int keyEnd = -1;

            while (i < len) {
                byte c = raw[i];
                if (c == '=' && keyEnd == -1) keyEnd = i;
                else if (c == '&') break;
                i++;
            }

            int end = i;

            if (keyEnd > keyStart) {
                byte[] key = Arrays.copyOfRange(raw, keyStart, keyEnd);

                Slice value = new Slice(
                    raw,
                    keyEnd + 1,
                    end - keyEnd - 1
                );

                putInternal(key, hash(key), value);
            }

            i++;
        }
    }

    private void putInternal(byte[] key, int hash, Slice value) {
        if (needsResize()) resize();

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

    private static int hash(byte[] key) {
        int h = 1;
        for (byte b : key) {
            h = 31 * h + (b & 0xFF);
        }
        return h;
    }

    private int find(byte[] key) {
        int h = hash(key);
        int idx = h & mask;

        byte[][] keys = this.keys;
        int[] hashes = this.hashes;
        while (true) {
            byte[] k = keys[idx];
            if (k == null) return -1;

            if (hashes[idx] == h && equals(k, key)) {
                return idx;
            }

            idx = (idx + 1) & mask;
        }
    }

    private static boolean equals(byte[] a, byte[] b) {
        int length = a.length;
        if (length != b.length) return false;

        for (int i = 0; i < length; i++) {
            if (a[i] != b[i]) return false;
        }
        return true;
    }

    public String get(String key) {
        return get(key.getBytes(StandardCharsets.UTF_8));
    }

    public String get(byte[] key) {
        int idx = find(key);
        if (idx < 0) return null;

        if (decodedCache[idx][0] != null) {
            return decodedCache[idx][0];
        }

        return decodedCache[idx][0] =
            FastUrlDecoder.decode(values[idx][0]);
    }

    public int size(byte[] key) {
        int idx = find(key);
        return idx < 0 ? 0 : counts[idx];
    }

    public void forEach(QueryParamConsumer consumer) {
        byte[][] keys = this.keys;
        int[] counts = this.counts;
        Slice[][] values = this.values;
        int length = keys.length;
        for (int i = 0; i < length; i++) {
            byte[] key = keys[i];
            if (key == null) continue;

            int count = counts[i];
            Slice[] vals = values[i];

            for (int j = 0; j < count; j++) {
                consumer.accept(key, vals[j]);
            }
        }
    }

    public interface QueryParamConsumer {
        void accept(byte[] key, Slice value);
    }

    public Iterator iterator() {
        return new Iterator();
    }

    public final class Iterator {

        private int bucketIndex = -1;
        private int valueIndex = 0;

        private byte[] currentKey;
        private Slice[] currentValues;
        private int currentCount;

        public boolean next() {
            // Move within current bucket first
            if (currentKey != null && valueIndex < currentCount) {
                return true;
            }

            // Move to next bucket
            byte[][] keys = HttpQueryParamIndex.this.keys;
            int[] counts = HttpQueryParamIndex.this.counts;
            Slice[][] values = HttpQueryParamIndex.this.values;

            int len = keys.length;

            while (++bucketIndex < len) {
                byte[] key = keys[bucketIndex];
                if (key == null) continue;

                currentKey = key;
                currentValues = values[bucketIndex];
                currentCount = counts[bucketIndex];
                valueIndex = 0;

                return true;
            }

            return false;
        }

        public void advance() {
            valueIndex++;
        }

        public byte[] key() {
            return currentKey;
        }

        public Slice valueSlice() {
            return currentValues[valueIndex];
        }

        public String value() {
            String[][] cache = HttpQueryParamIndex.this.decodedCache;

            String cached = cache[bucketIndex][valueIndex];
            if (cached != null) return cached;

            return cache[bucketIndex][valueIndex] =
                FastUrlDecoder.decode(currentValues[valueIndex]);
        }
    }
}