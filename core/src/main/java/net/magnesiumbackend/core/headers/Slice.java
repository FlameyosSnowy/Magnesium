package net.magnesiumbackend.core.headers;

import java.nio.charset.StandardCharsets;

public record Slice(byte[] src, int start, int len) {
    public int length() {
        return len;
    }

    public byte byteAt(int i) {
        return src[start + i];
    }

    public char charAt(int i) {
        return (char) (src[start + i] & 255);
    }

    /** Only materialize when absolutely needed */
    public String materialize() {
        return new String(src, start, len, StandardCharsets.UTF_8);
    }

    /** Materialize as lowercase ASCII string in one allocation */
    public String materializeLowercase() {
        int n = len;
        char[] out = new char[n];

        byte[] src = this.src;
        for (int i = 0; i < n; i++) {
            byte b = src[start + i];

            if (b >= 'A' && b <= 'Z') {
                out[i] = (char) (b + 32);
            } else {
                out[i] = (char) (b & 0xFF);
            }
        }

        return new String(out);
    }

    public boolean equalsIgnoreCase(String other) {
        int n = len;
        if (other.length() != n) return false;

        byte[] src = this.src;
        for (int i = 0; i < n; i++) {
            byte a = src[start + i];
            char b = other.charAt(i);

            if (toLowerAscii(a) != toLowerAscii((byte) b)) return false;
        }
        return true;
    }

    private static byte toLowerAscii(byte b) {
        return (b >= 'A' && b <= 'Z') ? (byte) (b + 32) : b;
    }

    public boolean isBlank() {
        int n = len;
        if (n == 0) return true;

        byte[] src = this.src;
        for (int i = 0; i < n; i++) {
            byte b = src[start + i];

            // fast ASCII whitespace check
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
}