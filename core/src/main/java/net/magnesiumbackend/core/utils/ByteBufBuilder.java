package net.magnesiumbackend.core.utils;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class ByteBufBuilder {

    private byte[] buf;
    private int pos;

    public ByteBufBuilder(int initialCapacity) {
        this.buf = new byte[initialCapacity];
    }


    public ByteBufBuilder() {
        this.buf = new byte[16];
    }

    public void append(@NotNull String s) {
        int len = s.length();
        ensure(len);

        for (int i = 0; i < len; i++) {
            buf[pos++] = (byte) s.charAt(i);
        }
    }

    public void appendAscii(@NotNull String s) {
        int len = s.length();
        ensure(len);

        s.getBytes(0, len, buf, pos);
        pos += len;
    }

    public void append(byte b) {
        ensure(1);
        buf[pos++] = b;
    }

    public void append(byte[] bytes) {
        ensure(1);
        System.arraycopy(bytes, 0, buf, pos, bytes.length);
        pos += bytes.length;
    }



    @Contract(value = " -> new", pure = true)
    public byte @NotNull [] build() {
        return buf;
    }

    private void ensure(int needed) {
        int required = pos + needed;
        if (required > buf.length) {
            int newCap = Math.max(buf.length << 1, required);
            buf = Arrays.copyOf(buf, newCap);
        }
    }

    public void setLength(int length) {
        pos = length;
    }

    public void append(byte[] array, int index, int length) {
        ensure(length);
        System.arraycopy(array, index, buf, pos, length);
        pos += length;
    }
}