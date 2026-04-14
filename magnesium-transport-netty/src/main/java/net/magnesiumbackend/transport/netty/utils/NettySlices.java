package net.magnesiumbackend.transport.netty.utils;

import io.netty.util.AsciiString;
import net.magnesiumbackend.core.headers.Slice;

public final class NettySlices {

    private NettySlices() {}

    public static Slice of(CharSequence cs) {
        if (cs instanceof AsciiString ascii) {
            return new Slice(
                ascii.array(),
                ascii.arrayOffset(),
                ascii.length()
            );
        }

        int len = cs.length();
        byte[] out = new byte[len];

        for (int i = 0; i < len; i++) {
            out[i] = (byte) cs.charAt(i);
        }

        return new Slice(out, 0, len);
    }

    public static Slice of(CharSequence cs, int start, int end) {
        if (cs instanceof AsciiString ascii) {
            return new Slice(
                ascii.array(),
                ascii.arrayOffset() + start,
                end - start
            );
        }

        int len = end - start;
        byte[] out = new byte[len];

        for (int i = 0; i < len; i++) {
            out[i] = (byte) cs.charAt(start + i);
        }

        return new Slice(out, 0, len);
    }
}