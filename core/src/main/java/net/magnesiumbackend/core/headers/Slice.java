package net.magnesiumbackend.core.headers;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

public class Slice implements CharSequence {
    private final byte[] src;
    private final int start;
    private final int len;

    private transient int hash;
    private transient boolean hashed;
    private transient String materialized; // Cache for materialize()

    public Slice(byte[] src, int start, int len) {
        this.src = src;
        this.start = start;
        this.len = len;
    }

    @Contract("_ -> new")
    public static @NotNull Slice of(String key) {
        return new Slice(key.getBytes(StandardCharsets.UTF_8), 0, key.length());
    }

    public byte[] src() {
        return src;
    }

    public int start() {
        return start;
    }

    public int len() {
        return len;
    }

    public byte byteAt(int i) {
        if (i < 0 || i >= len) {
            throw new IndexOutOfBoundsException();
        }
        return src[start + i];
    }

    @Override
    public int hashCode() {
        if (hashed) return hash;

        int h = 0x811c9dc5; // FNV-1a
        int end = start + len;

        byte[] s = src;

        for (int i = start; i < end; i++) {
            h ^= s[i];
            h *= 0x01000193;
        }

        hash = h;
        hashed = true;
        return h;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Slice other)) return false;
        if (other.len != this.len) return false;

        byte[] a = this.src;
        byte[] b = other.src;

        int i = this.start;
        int j = other.start;
        int end = i + len;

        for (; i < end; i++, j++) {
            if (a[i] != b[j]) return false;
        }

        return true;
    }

    /**
     * Converts this slice to a String, caching the result.
     *
     * <p>First call performs UTF-8 decoding and caches the result.
     * Subsequent calls return the cached String.</p>
     *
     * @return the string representation of this slice
     */
    public String materialize() {
        String s = materialized;
        if (s == null) {
            s = new String(src, start, len, StandardCharsets.UTF_8);
            materialized = s;
        }
        return s;
    }

    public String materializeLowercase() {
        char[] out = new char[len];

        for (int i = 0; i < len; i++) {
            byte b = src[start + i];

            if (b >= 'A' && b <= 'Z') {
                out[i] = (char) (b + 32);
            } else {
                out[i] = (char) (b & 0xFF);
            }
        }

        return new String(out);
    }

    // ----------------------------
    // Comparisons
    // ----------------------------

    public boolean equalsIgnoreCase(String other) {
        if (other.length() != len) return false;

        for (int i = 0; i < len; i++) {
            byte a = src[start + i];
            char b = other.charAt(i);

            if (toLowerAscii(a) != toLowerAscii((byte) b)) {
                return false;
            }
        }
        return true;
    }

    private static byte toLowerAscii(byte b) {
        return (b >= 'A' && b <= 'Z') ? (byte) (b + 32) : b;
    }

    public boolean isBlank() {
        for (int i = 0; i < len; i++) {
            byte b = src[start + i];
            if (!(b == ' ' || b == '\t' || b == '\n' || b == '\r')) {
                return false;
            }
        }
        return true;
    }

    public Slice slice(int from, int to) {
        int newLen = to - from;
        if (from < 0 || to > len || newLen < 0) {
            throw new IndexOutOfBoundsException();
        }
        return new Slice(src, start + from, newLen);
    }

    public Slice slice(int from) {
        if (from < 0 || from > len) {
            throw new IndexOutOfBoundsException();
        }
        return new Slice(src, start + from, len - from);
    }

    public int parseInt() {
        int i = start;
        int end = start + len;

        int sign = 1;
        if (src[i] == '-') {
            sign = -1;
            i++;
        }

        int val = 0;
        for (; i < end; i++) {
            val = val * 10 + (src[i] - '0');
        }

        return val * sign;
    }

    public long parseLong() {
        int i = start;
        int end = start + len;

        long sign = 1;
        if (src[i] == '-') {
            sign = -1;
            i++;
        }

        long val = 0;
        for (; i < end; i++) {
            val = val * 10 + (src[i] - '0');
        }

        return val * sign;
    }

    /**
     * Parses:
     *  - 12.34
     *  - -12.34
     *  - 12
     */
    public double parseDouble() {
        int i = start;
        int end = start + len;

        double sign = 1;
        if (src[i] == '-') {
            sign = -1;
            i++;
        }

        long intPart = 0;
        while (i < end && src[i] != '.' && src[i] != 'e' && src[i] != 'E') {
            intPart = intPart * 10 + (src[i] - '0');
            i++;
        }

        double result = intPart;

        // fractional
        if (i < end && src[i] == '.') {
            i++;
            double div = 1.0;

            while (i < end && src[i] >= '0' && src[i] <= '9') {
                result = result * 10 + (src[i] - '0');
                div *= 10;
                i++;
            }

            result /= div;
        }

        // exponent
        if (i < end && (src[i] == 'e' || src[i] == 'E')) {
            i++;
            int expSign = 1;
            if (src[i] == '-') {
                expSign = -1;
                i++;
            } else if (src[i] == '+') {
                i++;
            }

            int exp = 0;
            while (i < end) {
                exp = exp * 10 + (src[i] - '0');
                i++;
            }

            result *= Math.pow(10, exp * expSign);
        }

        return result * sign;
    }

    /**
     * Fixed-point parsing (e.g. "123.45" with scale 2 → 12345)
     */
    public long parseFixedPoint(int scale) {
        long val = 0;
        int seen = 0;

        for (int i = start; i < start + len; i++) {
            byte b = src[i];

            if (b == '.') continue;

            val = val * 10 + (b - '0');
            seen++;
        }

        while (seen < scale) {
            val *= 10;
            seen++;
        }

        return val;
    }

    public static Slice of(int value) {
        return of(String.valueOf(value));
    }

    public static Slice of(long value) {
        return of(String.valueOf(value));
    }

    public static Slice of(double value) {
        return of(String.valueOf(value));
    }

    public static Slice of(byte[] bytes) {
        return new Slice(bytes, 0, bytes.length);
    }

    // CharSequence

    @Override
    public int length() {
        return len;
    }

    @Override
    public char charAt(int i) {
        if (i < 0 || i >= len) {
            throw new IndexOutOfBoundsException();
        }
        return (char) (src[start + i] & 0xFF);
    }

    @Override
    public @NotNull CharSequence subSequence(int start, int end) {
        int newLen = end - start;
        if (start < 0 || end > len || newLen < 0) {
            throw new IndexOutOfBoundsException();
        }
        return new Slice(src, this.start + start, newLen);
    }

    @Override
    public @NotNull String toString() {
        return materialize();
    }
}