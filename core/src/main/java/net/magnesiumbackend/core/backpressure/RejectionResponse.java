package net.magnesiumbackend.core.backpressure;

import net.magnesiumbackend.core.http.response.ResponseEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;

/**
 * Describes the HTTP response that is sent when a request is rejected due to backpressure.
 *
 * <p>Create instances via the static factories:
 * <pre>{@code
 * // Simplest — 503 with no body
 * RejectionResponse.of(503)
 *
 * // 503 with Retry-After
 * RejectionResponse.of(503).withRetryAfter(Duration.ofSeconds(5))
 *
 * // 429 with body
 * RejectionResponse.of(429).withBody("Too many requests, slow down.")
 *
 * // Fully custom — bring your own ResponseEntity
 * RejectionResponse.custom(req -> ResponseEntity.status(503).header("X-Queue-Full", "true").build())
 * }</pre>
 */
public final class RejectionResponse {

    private final int statusCode;
    @Nullable private final String body;
    @Nullable private final Duration retryAfter;

    private RejectionResponse(int statusCode, @Nullable String body, @Nullable Duration retryAfter) {
        if (statusCode < 100 || statusCode > 599) {
            throw new IllegalArgumentException("Invalid HTTP status code: " + statusCode);
        }
        this.statusCode = statusCode;
        this.body       = body;
        this.retryAfter = retryAfter;
    }

    // -------------------------------------------------------------------------
    // Factories
    // -------------------------------------------------------------------------

    /** 503 Service Unavailable with no body and no Retry-After. */
    public static RejectionResponse serviceUnavailable() {
        return new RejectionResponse(503, null, null);
    }

    /** 429 Too Many Requests with no body and no Retry-After. */
    public static RejectionResponse tooManyRequests() {
        return new RejectionResponse(429, null, null);
    }

    /** Any status code. */
    public static RejectionResponse of(int statusCode) {
        return new RejectionResponse(statusCode, null, null);
    }

    // -------------------------------------------------------------------------
    // Fluent builders
    // -------------------------------------------------------------------------

    /** Attaches a plain-text body. */
    public RejectionResponse withBody(@NotNull String body) {
        return new RejectionResponse(this.statusCode, body, this.retryAfter);
    }

    /**
     * Adds a {@code Retry-After} header (integer seconds).
     * Ignored if the duration is zero or negative.
     */
    public RejectionResponse withRetryAfter(@NotNull Duration retryAfter) {
        return new RejectionResponse(this.statusCode, this.body, retryAfter);
    }

    // -------------------------------------------------------------------------
    // Accessors (used by transport layer to build the actual HTTP response)
    // -------------------------------------------------------------------------

    public int statusCode() { return statusCode; }

    @Nullable public String body() { return body; }

    @Nullable public Duration retryAfter() { return retryAfter; }

    /**
     * Returns the {@code Retry-After} value as whole seconds, or {@code -1} if not set.
     * Transports should skip the header entirely when this returns {@code -1}.
     */
    public long retryAfterSeconds() {
        if (retryAfter == null || retryAfter.isNegative() || retryAfter.isZero()) return -1L;
        return retryAfter.getSeconds();
    }

    @Override
    public String toString() {
        return "RejectionResponse{status=" + statusCode
            + (body       != null ? ", body='" + body + '\'' : "")
            + (retryAfter != null ? ", retryAfter=" + retryAfter : "")
            + '}';
    }
}