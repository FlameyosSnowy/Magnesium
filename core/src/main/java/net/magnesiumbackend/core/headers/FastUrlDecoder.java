package net.magnesiumbackend.core.headers;

import java.nio.charset.StandardCharsets;

final class FastUrlDecoder {

    private FastUrlDecoder() {}

    public static String decode(Slice slice) {
        byte[] src = slice.src();
        int start = slice.start();
        int len = slice.len();

        byte[] out = new byte[len];
        int oi = 0;

        for (int i = 0; i < len; i++) {
            byte b = src[start + i];

            if (b == '+') {
                out[oi++] = ' ';
                continue;
            }

            if (b == '%' && i + 2 < len) {
                int hi = hex(src[start + i + 1]);
                int lo = hex(src[start + i + 2]);

                if (hi >= 0 && lo >= 0) {
                    out[oi++] = (byte) ((hi << 4) | lo);
                    i += 2;
                    continue;
                }
            }

            out[oi++] = b;
        }

        return new String(out, 0, oi, StandardCharsets.UTF_8);
    }

    private static int hex(byte b) {
        if (b >= '0' && b <= '9') return b - '0';
        if (b >= 'A' && b <= 'F') return b - 'A' + 10;
        if (b >= 'a' && b <= 'f') return b - 'a' + 10;
        return -1;
    }
}