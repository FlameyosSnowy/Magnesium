package net.magnesiumbackend.core.security;

import net.magnesiumbackend.core.headers.HttpPathParamIndex;
import net.magnesiumbackend.core.headers.HttpQueryParamIndex;
import net.magnesiumbackend.core.headers.Slice;
import net.magnesiumbackend.core.http.response.ErrorResponse;
import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.route.FilterChain;
import net.magnesiumbackend.core.route.HttpFilter;
import net.magnesiumbackend.core.route.RequestContext;

/**
 * Security filter that detects and blocks path traversal attacks.
 *
 * <p>PathTraversalFilter inspects path variables and query parameters for
 * malicious sequences that could escape the intended directory:
 * <ul>
 *   <li>{@code ..} - Parent directory references</li>
 *   <li>{@code ../} - Path separators</li>
 *   <li>Null bytes ({@code \0}) - String termination attacks</li>
 *   <li>Control characters</li>
 *   <li>Unicode homoglyphs via NFKC normalization</li>
 * </ul>
 * </p>
 *
 * <p>The filter performs multi-pass URL decoding to catch encoded attacks.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * MagnesiumApplication.builder()
 *     .filter(new PathTraversalFilter())
 *     .build();
 * }</pre>
 *
 * @see HttpFilter
 */
public final class PathTraversalFilter implements HttpFilter {

    /** Maximum number of URL decode passes to prevent abuse. */
    private static final int MAX_DECODE_PASSES = 3;

    @Override
    public Object handle(RequestContext ctx, FilterChain chain) {
        // path variables
        for (HttpPathParamIndex.Entry entry : ctx.pathVariables()) {
            if (isDangerous(entry.value())) {
                return bad("Path variable contains illegal sequence.");
            }
        }

        // query params
        HttpQueryParamIndex httpQueryParamIndex = ctx.request().queryParams();
        HttpQueryParamIndex.Iterator iterator = httpQueryParamIndex.iterator();
        while (iterator.next()) {
            Slice value = iterator.valueSlice();
            if (isDangerous(value)) {
                return bad("Query parameter contains illegal sequence.");
            }
            iterator.advance();
        }

        return chain.next(ctx);
    }

    private ResponseEntity<?> bad(String msg) {
        return ResponseEntity.of(400, new ErrorResponse("invalid_path", msg));
    }

    private boolean isDangerous(Slice slice) {
        if (slice == null || slice.len() == 0) return false;

        byte[] src = slice.src();
        int start = slice.start();
        int end = start + slice.len();

        int dots = 0;

        for (int i = start; i < end; i++) {
            byte b = src[i];

            // fast lowercase
            if (b >= 'A' && b <= 'Z') b += 32;

            // normalize slashes
            if (b == '\\') b = '/';

            // null byte
            if (b == 0) return true;

            // control chars
            if (b < 32) return true;

            if (b == '.') {
                dots++;
                if (dots >= 2) return true;
                continue;
            }

            if (b == '/') {
                if (dots >= 2) return true;
                dots = 0;
                continue;
            }

            dots = 0;

            // encoded attack detection trigger
            if (b == '%' || b == '+' || (b & 0x80) != 0) {
                return slowPath(slice);
            }
        }

        return false;
    }

    private boolean slowPath(Slice slice) {
        char[] buf = toCharArray(slice);

        int len = multiDecode(buf);

        String normalized = java.text.Normalizer.normalize(
            new String(buf, 0, len),
            java.text.Normalizer.Form.NFKC
        );

        return checkNormalized(normalized);
    }

    private char[] toCharArray(Slice slice) {
        byte[] src = slice.src();
        int start = slice.start();
        int len = slice.len();

        char[] out = new char[len];

        for (int i = 0; i < len; i++) {
            out[i] = (char) (src[start + i] & 0xFF);
        }

        return out;
    }

    private boolean checkNormalized(String input) {
        int dots = 0;
        int len = input.length();

        for (int i = 0; i < len; i++) {
            char c = input.charAt(i);

            if (c >= 'A' && c <= 'Z') c += 32;
            if (c == '\\') c = '/';

            if (c == 0) return true;
            if (c < 32) return true;

            if (c == '.') {
                dots++;
                if (dots >= 2) return true;
                continue;
            }

            if (c == '/') {
                if (dots >= 2) return true;
                dots = 0;
                continue;
            }

            dots = 0;
        }

        return false;
    }

    /**
     * Multi-pass decode (bounded to avoid abuse)
     */
    private int multiDecode(char[] buf) {
        int len = buf.length;

        for (int i = 0; i < MAX_DECODE_PASSES; i++) {
            int newLen = decodeOnce(buf, len);
            if (newLen == len) break;
            len = newLen;
        }

        return len;
    }

    /**
     * Single-pass %xx decode in-place
     */
    private int decodeOnce(char[] buf, int len) {
        int write = 0;

        for (int read = 0; read < len; read++) {
            char c = buf[read];

            if (c == '%' && read + 2 < len) {
                int hi = hex(buf[read + 1]);
                int lo = hex(buf[read + 2]);

                if (hi >= 0 && lo >= 0) {
                    buf[write++] = (char) ((hi << 4) | lo);
                    read += 2;
                    continue;
                }
            }

            buf[write++] = c;
        }

        return write;
    }

    private int hex(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        return -1;
    }
}