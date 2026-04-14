package net.magnesiumbackend.core.headers;

public final class DecimalCodec {

    private DecimalCodec() {}

    public static long parseFixed(byte[] src, int start, int len, int scaleOut) {

        int i = start;
        int end = start + len;

        long sign = 1;
        long result = 0;
        int scale = 0;
        boolean frac = false;

        if (i < end && src[i] == '-') {
            sign = -1;
            i++;
        }

        while (i < end) {
            byte b = src[i++];

            if (b == '.') {
                frac = true;
                continue;
            }

            if (b == 'e' || b == 'E') {
                int exp = parseInt(src, i, end - i);
                return applyExponent(result, scale, exp, sign);
            }

            if (b < '0' || b > '9') {
                throw new NumberFormatException("Invalid decimal");
            }

            result = result * 10 + (b - '0');

            if (frac) scale++;
        }

        while (scale < scaleOut) {
            result *= 10;
            scale++;
        }

        return sign * result;
    }

    private static int parseInt(byte[] src, int start, int len) {
        int sign = 1;
        int r = 0;

        int i = start;
        int end = start + len;

        if (i < end && src[i] == '-') {
            sign = -1;
            i++;
        }

        while (i < end) {
            byte b = src[i++];
            if (b < '0' || b > '9') break;
            r = r * 10 + (b - '0');
        }

        return r * sign;
    }

    private static long parseLong(byte[] src, int start, int len) {
        long sign = 1;
        long r = 0;

        int i = start;
        int end = start + len;

        if (i < end && src[i] == '-') {
            sign = -1;
            i++;
        }

        while (i < end) {
            byte b = src[i++];
            if (b < '0' || b > '9') break;
            r = r * 10 + (b - '0');
        }

        return r * sign;
    }

    private static long applyExponent(long value, int scale, int exp, long sign) {
        long v = value * sign;

        if (exp > 0) {
            for (int i = 0; i < exp; i++) v *= 10;
        } else {
            for (int i = 0; i < -exp; i++) v /= 10;
        }

        return v;
    }

    // =========================
    // FORMAT (fixed-point -> bytes)
    // =========================

    public static byte[] formatFixed(long value, int scale) {

        boolean neg = value < 0;
        long v = neg ? -value : value;

        byte[] tmp = new byte[32];
        int pos = tmp.length;

        int i = 0;

        while (i < scale) {
            tmp[--pos] = (byte) ('0' + (v % 10));
            v /= 10;
            i++;
        }

        tmp[--pos] = '.';

        do {
            tmp[--pos] = (byte) ('0' + (v % 10));
            v /= 10;
        } while (v > 0);

        if (neg) tmp[--pos] = '-';

        int len = tmp.length - pos;
        byte[] out = new byte[len];
        System.arraycopy(tmp, pos, out, 0, len);

        return out;
    }
}