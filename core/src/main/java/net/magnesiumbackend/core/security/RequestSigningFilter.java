package net.magnesiumbackend.core.security;

import net.magnesiumbackend.core.http.ResponseEntity;
import net.magnesiumbackend.core.route.FilterChain;
import net.magnesiumbackend.core.route.HttpFilter;
import net.magnesiumbackend.core.route.RequestContext;
import net.magnesiumbackend.core.security.exceptions.SignatureException;
import net.magnesiumbackend.core.headers.Slice;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

public final class RequestSigningFilter implements HttpFilter {

    private static final String SIGNATURE_HEADER = "X-Signature-SHA256";
    private static final String TIMESTAMP_HEADER  = "X-Timestamp";
    private static final long CLOCK_SKEW_MS      = 5 * 60 * 1000L;

    private final SecretResolver secretResolver;

    @FunctionalInterface
    public interface SecretResolver {
        String resolve(RequestContext ctx);
    }

    // ThreadLocal Mac reuse (critical perf win)
    private static final ThreadLocal<Mac> MAC = ThreadLocal.withInitial(() -> {
        try {
            return Mac.getInstance("HmacSHA256");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    });

    public RequestSigningFilter(SecretResolver secretResolver) {
        this.secretResolver = secretResolver;
    }

    @Override
    public ResponseEntity<?> handle(RequestContext ctx, FilterChain chain) {

        String secret = secretResolver.resolve(ctx);
        if (secret == null) {
            return chain.next(ctx);
        }

        verify(ctx, secret);
        return chain.next(ctx);
    }

    private void verify(RequestContext ctx, String secret) {

        Slice signature = ctx.header(SIGNATURE_HEADER);
        Slice timestamp = ctx.header(TIMESTAMP_HEADER);

        if (signature == null || timestamp == null) {
            throw new SignatureException("Missing signature or timestamp header");
        }

        String tsStr = timestamp.materialize();
        validateTimestamp(tsStr);

        String body = ctx.request().body();
        String payload = tsStr + "." + body;

        String expected = hmac(secret, payload);

        // constant-time compare without allocations
        if (!constantTimeEquals(expected, signature)) {
            throw new SignatureException("Invalid signature");
        }
    }

    private void validateTimestamp(String timestamp) {
        try {
            long ts = Long.parseLong(timestamp);
            long now = System.currentTimeMillis();

            if (Math.abs(now - ts) > CLOCK_SKEW_MS) {
                throw new SignatureException("Timestamp outside acceptable window");
            }

        } catch (NumberFormatException e) {
            throw new SignatureException("Malformed timestamp");
        }
    }

    private static String hmac(String secret, String payload) {
        try {
            Mac mac = MAC.get();
            mac.reset();

            mac.init(new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            ));

            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);

        } catch (Exception e) {
            throw new IllegalStateException("HMAC computation failed", e);
        }
    }

    private static boolean constantTimeEquals(String expected, Slice actual) {

        if (expected.length() != actual.len()) return false;

        int start = actual.start();

        int result = 0;

        for (int i = 0; i < expected.length(); i++) {
            result |= expected.charAt(i) ^ actual.charAt(start + i);
        }

        return result == 0;
    }
}