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

    public void append(@NotNull String s) {
        int len = s.length();
        ensure(len);

        for (int i = 0; i < len; i++) {
            buf[pos++] = (byte) s.charAt(i);
        }
    }

    public void append(byte b) {
        ensure(1);
        buf[pos++] = b;
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
}