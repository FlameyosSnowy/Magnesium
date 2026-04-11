package net.magnesiumbackend.core.security;

import net.magnesiumbackend.core.http.ErrorResponse;
import net.magnesiumbackend.core.http.ResponseEntity;
import net.magnesiumbackend.core.route.FilterChain;
import net.magnesiumbackend.core.route.HttpFilter;
import net.magnesiumbackend.core.route.RequestContext;

import java.text.Normalizer;
import java.util.Map;

public final class PathTraversalFilter implements HttpFilter {

    private static final int MAX_DECODE_PASSES = 3;

    @Override
    public ResponseEntity<?> handle(RequestContext ctx, FilterChain chain) {
        // path variables
        for (Map.Entry<String, String> entry : ctx.pathVariables().entrySet()) {
            if (isDangerous(entry.getValue())) {
                return bad("Path variable contains illegal sequence.");
            }
        }

        // query params
        for (Map.Entry<String, String> entry : ctx.request().queryParams().entrySet()) {
            if (isDangerous(entry.getValue())) {
                return bad("Query parameter contains illegal sequence.");
            }
        }

        return chain.next(ctx);
    }

    private ResponseEntity<?> bad(String msg) {
        return ResponseEntity.of(400, new ErrorResponse("invalid_path", msg));
    }

    private boolean isDangerous(String input) {
        if (input == null || input.isEmpty()) return false;

        // Unicode normalization (kills homoglyph tricks)
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFKC);

        char[] buf = normalized.toCharArray();
        int len = multiDecode(buf);

        int dots = 0;

        for (int i = 0; i < len; i++) {
            char c = buf[i];

            // normalize case
            if (c >= 'A' && c <= 'Z') c += 32;

            // normalize slashes
            if (c == '\\') c = '/';

            // null byte
            if (c == 0) return true;

            // control chars
            if (c < 32) return true;

            if (c == '.') {
                dots++;
                if (dots >= 2) return true; // ".."
                continue;
            }

            if (c == '/') {
                if (dots >= 2) return true; // "../"
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