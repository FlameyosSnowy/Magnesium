package net.magnesiumbackend.core.security;

import net.magnesiumbackend.core.route.FilterChain;
import net.magnesiumbackend.core.route.HttpFilter;
import net.magnesiumbackend.core.route.RequestContext;
import net.magnesiumbackend.core.security.exceptions.SignatureException;
import net.magnesiumbackend.core.headers.Slice;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * HTTP filter that verifies HMAC-SHA256 request signatures.
 *
 * <p>RequestSigningFilter provides request integrity verification for webhooks
 * and API endpoints. It validates:
 * <ul>
 *   <li>Signature header presence and format</li>
 *   <li>Timestamp freshness (within 5 minute window)</li>
 *   <li>HMAC-SHA256 signature match using configured secret</li>
 * </ul>
 * </p>
 *
 * <h3>Expected Headers</h3>
 * <ul>
 *   <li>{@code X-Signature-SHA256} - Hex-encoded HMAC signature</li>
 *   <li>{@code X-Timestamp} - Request timestamp in milliseconds</li>
 * </ul>
 *
 * <h3>Signature Format</h3>
 * <pre>HMAC-SHA256(timestamp + "." + body)</pre>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * MagnesiumApplication.builder()
 *     .filter(new RequestSigningFilter(ctx -> {
 *         // Resolve secret based on request (e.g., by API key)
 *         return apiKeyService.getSecret(ctx.header("X-API-Key"));
 *     }))
 *     .build();
 * }</pre>
 *
 * @see HttpFilter
 * @see SignatureException
 */
public final class RequestSigningFilter implements HttpFilter {

    /** Header containing the HMAC-SHA256 signature. */
    private static final String SIGNATURE_HEADER = "X-Signature-SHA256";

    /** Header containing the request timestamp. */
    private static final String TIMESTAMP_HEADER  = "X-Timestamp";

    /** Allowed clock skew: 5 minutes. */
    private static final long CLOCK_SKEW_MS      = 5 * 60 * 1000L;

    private final SecretResolver secretResolver;

    /**
     * Resolves the HMAC secret for a given request.
     */
    @FunctionalInterface
    public interface SecretResolver {
        /**
         * Returns the secret key for signature verification.
         *
         * @param ctx the request context
         * @return the secret key, or null if signature verification not required
         */
        String resolve(RequestContext ctx);
    }

    /** ThreadLocal Mac instance for performance (HmacSHA256 is not thread-safe). */
    private static final ThreadLocal<Mac> MAC = ThreadLocal.withInitial(() -> {
        try {
            return Mac.getInstance("HmacSHA256");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    });

    /**
     * Creates a new request signing filter.
     *
     * @param secretResolver the secret resolver for HMAC verification
     */
    public RequestSigningFilter(SecretResolver secretResolver) {
        this.secretResolver = secretResolver;
    }

    @Override
    public Object handle(RequestContext ctx, FilterChain chain) {

        String secret = secretResolver.resolve(ctx);
        if (secret == null) {
            return chain.next(ctx);
        }

        verify(ctx, secret);
        return chain.next(ctx);
    }

    private void verify(RequestContext ctx, String secret) {

        Slice signature = ctx.headerRaw(SIGNATURE_HEADER);
        Slice timestamp = ctx.headerRaw(TIMESTAMP_HEADER);

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