package net.magnesiumbackend.core.security;

import org.jetbrains.annotations.NotNull;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

/**
 * CSRF token for protection against cross-site request forgery attacks.
 *
 * <p>CsrfToken encapsulates a cryptographically random token value with
 * expiration tracking. Tokens are generated using {@link SecureRandom} for
 * sufficient entropy.</p>
 *
 * <h3>Token Format</h3>
 * <ul>
 *   <li>32 bytes of random data (256 bits entropy)</li>
 *   <li>Base64URL encoded for safe transport</li>
 *   <li>44 characters when encoded</li>
 * </ul>
 *
 * @see CsrfFilter
 * @see CsrfConfig
 */
public final class CsrfToken {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final String value;
    private final Instant createdAt;
    private final long ttlSeconds;

    private CsrfToken(String value, Instant createdAt, long ttlSeconds) {
        this.value = value;
        this.createdAt = createdAt;
        this.ttlSeconds = ttlSeconds;
    }

    /**
     * Generates a new CSRF token with default TTL.
     *
     * @return a new random CSRF token
     */
    public static @NotNull CsrfToken generate() {
        return generate(3600); // 1 hour default
    }

    /**
     * Generates a new CSRF token with specified TTL.
     *
     * @param ttlSeconds token time-to-live in seconds
     * @return a new random CSRF token
     */
    public static @NotNull CsrfToken generate(long ttlSeconds) {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new CsrfToken(value, Instant.now(), ttlSeconds);
    }

    /**
     * Creates a token from an existing value (e.g., from cookie).
     *
     * @param value the token value
     * @return token instance without expiration tracking
     */
    public static @NotNull CsrfToken of(@NotNull String value) {
        return new CsrfToken(value, Instant.now(), Long.MAX_VALUE);
    }

    /**
     * Returns the token value.
     *
     * @return the Base64URL-encoded token
     */
    public @NotNull String value() {
        return value;
    }

    /**
     * Checks if the token has expired.
     *
     * @return true if token is expired
     */
    public boolean isExpired() {
        return Instant.now().isAfter(createdAt.plusSeconds(ttlSeconds));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CsrfToken that)) return false;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return "CsrfToken[expires=" + createdAt.plusSeconds(ttlSeconds) + "]";
    }
}
