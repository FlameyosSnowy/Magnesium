package net.magnesiumbackend.core.headers;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class HttpPathParamIndex implements Iterable<HttpPathParamIndex.Entry> {

    private final Slice[] keys;
    private final Slice[] values;

    private final int mask;
    private final int size;

    private HttpPathParamIndex(Slice[] keys, Slice[] values, int size, int capacityMask) {
        this.keys = keys;
        this.values = values;
        this.size = size;
        this.mask = capacityMask;
    }

    public static HttpPathParamIndex of(Slice[] inputKeys, Slice[] inputValues) {
        int length = inputKeys.length;
        int capacity = tableSize(length);
        int mask = capacity - 1;

        Slice[] keys = new Slice[capacity];
        Slice[] values = new Slice[capacity];

        int size = 0;

        for (int i = 0; i < length; i++) {
            Slice k = inputKeys[i];
            Slice v = inputValues[i];

            int pos = indexFor(k, mask);

            while (keys[pos] != null) {
                if (equals(keys[pos], k)) {
                    values[pos] = v;
                    break;
                }
                pos = (pos + 1) & mask;
            }

            if (keys[pos] == null) {
                keys[pos] = k;
                values[pos] = v;
                size++;
            }
        }

        return new HttpPathParamIndex(keys, values, size, mask);
    }

    public static HttpPathParamIndex empty() {
        return new HttpPathParamIndex(new Slice[0], new Slice[0], 0, 0);
    }

    public Slice get(Slice key) {
        if (keys.length == 0) return null;

        int pos = indexFor(key, mask);

        while (true) {
            Slice k = keys[pos];
            if (k == null) return null;

            if (equals(k, key)) {
                return values[pos];
            }

            pos = (pos + 1) & mask;
        }
    }

    public Slice getRaw(String key) {
        return get(Slice.of(key));
    }

    public String get(String key) {
        Slice value = getRaw(key);
        if (value == null) return null;
        return new String(value.src(), value.start(), value.length());
    }

    private static int indexFor(Slice key, int mask) {
        return hash(key) & mask;
    }

    private static int hash(Slice slice) {
        byte[] data = slice.src();
        int off = slice.start();
        int len = slice.length();

        int h = 1;

        for (int i = 0; i < len; i++) {
            h = 31 * h + data[off + i];
        }

        return h;
    }

    private static boolean equals(Slice a, Slice b) {
        if (a.length() != b.length()) return false;

        byte[] ad = a.src();
        byte[] bd = b.src();

        int ao = a.start();
        int bo = b.start();
        int len = a.length();

        for (int i = 0; i < len; i++) {
            if (ad[ao + i] != bd[bo + i]) return false;
        }

        return true;
    }

    private static int tableSize(int expected) {
        int n = 1;
        int target = (int) (expected / 0.7f) + 1;

        while (n < target) n <<= 1;

        return n;
    }

    public int size() {
        return size;
    }

    @Override
    public @NotNull Iterator<Entry> iterator() {
        return new EntryIterator(size, keys, values);
    }

    public record Entry(Slice key, Slice value) {}

    private static class EntryIterator implements Iterator<Entry> {
        int i = 0;

        private final int size;
        private final Slice[] keys;
        private final Slice[] values;

        private EntryIterator(int size, Slice[] keys, Slice[] values) {
            this.size = size;
            this.keys = keys;
            this.values = values;
        }

        @Override
        public boolean hasNext() {
            int length = size;
            while (i < length && keys[i] == null) i++;
            return i < length;
        }

        @Override
        public Entry next() {
            if (!hasNext()) throw new NoSuchElementException();
            return new Entry(keys[i], values[i++]);
        }
    }
}