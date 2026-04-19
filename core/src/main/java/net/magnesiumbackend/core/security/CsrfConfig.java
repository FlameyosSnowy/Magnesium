package net.magnesiumbackend.core.security;

import net.magnesiumbackend.core.http.response.HttpMethod;

import java.nio.charset.StandardCharsets;
import java.util.*;

public final class CsrfConfig {

    private final byte[] cookieNameEq;     // "XSRF-TOKEN="
    private final byte[] pathFragment;     // "; Path=/"
    private final byte[] sameSiteFragment; // "; SameSite=Lax"
    private final byte[] httpOnlyFragment; // "; HttpOnly" or empty
    private final byte[] secureFragment;   // "; Secure" or empty

    private final String headerName;
    private final String parameterName;
    private final long tokenTtlSeconds;

    private final Set<String> pathExclusions;
    private final EnumSet<HttpMethod> safeMethods;

    private final String cookieName;
    private final String cookiePath;
    private final SameSite sameSite;

    private CsrfConfig(Builder b) {
        this.cookieName = b.cookieName;
        this.cookiePath = b.cookiePath;
        this.sameSite = b.sameSite;

        this.headerName = b.headerName;
        this.parameterName = b.parameterName;
        this.tokenTtlSeconds = b.tokenTtlSeconds;
        this.pathExclusions = Set.copyOf(b.pathExclusions);
        this.safeMethods = EnumSet.copyOf(b.safeMethods);

        this.cookieNameEq = ascii(cookieName + "=");
        this.pathFragment = ascii("; Path=" + cookiePath);
        this.sameSiteFragment = sameSite.fragment;

        this.httpOnlyFragment = b.cookieHttpOnly
            ? ascii("; HttpOnly")
            : EMPTY;

        this.secureFragment = b.cookieSecure
            ? ascii("; Secure")
            : EMPTY;
    }



    /**
     * Creates a builder with secure defaults.
     *
     * @return builder with recommended settings
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Default configuration suitable for most web applications.
     *
     * @return default config
     */
    public static CsrfConfig defaults() {
        return new Builder().build();
    }

    /**
     * Strict configuration for high-security applications.
     *
     * @return strict config with Secure, SameSite=Strict, short TTL
     */
    public static CsrfConfig strict() {
        return new Builder()
            .cookieSecure(true)
            .sameSite(SameSite.STRICT)
            .tokenTtlSeconds(1800) // 30 minutes
            .build();
    }

    /**
     * Relaxed configuration for development or internal APIs.
     *
     * @return relaxed config with longer TTL, SameSite=Lax
     */
    public static CsrfConfig relaxed() {
        return new Builder()
            .sameSite(SameSite.LAX)
            .tokenTtlSeconds(7200) // 2 hours
            .build();
    }

    public String cookieName() { return cookieName; }
    public String cookiePath() { return cookiePath; }
    public SameSite sameSite() { return sameSite; }

    public byte[] cookieNameEq() { return cookieNameEq; }
    public byte[] pathFragment() { return pathFragment; }
    public byte[] sameSiteFragment() { return sameSiteFragment; }
    public byte[] httpOnlyFragment() { return httpOnlyFragment; }
    public byte[] secureFragment() { return secureFragment; }

    public String headerName() { return headerName; }
    public String parameterName() { return parameterName; }
    public long tokenTtlSeconds() { return tokenTtlSeconds; }

    public Set<String> pathExclusions() { return pathExclusions; }
    public EnumSet<HttpMethod> safeMethods() { return safeMethods; }

    // =========================
    // Matching
    // =========================

    public boolean isPathExcluded(String path) {
        for (String pattern : pathExclusions) {
            if (matchesPattern(path, pattern)) return true;
        }
        return false;
    }

    private boolean matchesPattern(String path, String pattern) {
        if (pattern.endsWith("/**")) {
            return path.startsWith(pattern.substring(0, pattern.length() - 3));
        }
        return path.equals(pattern);
    }

    public static class Builder {

        private String cookieName = "XSRF-TOKEN";
        private boolean cookieHttpOnly = false;
        private boolean cookieSecure = false;
        private SameSite sameSite = SameSite.LAX;
        private String cookiePath = "/";

        private String headerName = "X-XSRF-TOKEN";
        private String parameterName = "_csrf";
        private long tokenTtlSeconds = 3600;

        private Set<String> pathExclusions = new HashSet<>();
        private EnumSet<HttpMethod> safeMethods =
            EnumSet.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.OPTIONS);

        public Builder cookieName(String name) {
            this.cookieName = requireToken(name, "cookieName");
            return this;
        }

        public Builder cookieHttpOnly(boolean v) {
            this.cookieHttpOnly = v;
            return this;
        }

        public Builder cookieSecure(boolean v) {
            this.cookieSecure = v;
            return this;
        }

        public Builder sameSite(SameSite s) {
            this.sameSite = Objects.requireNonNull(s);
            return this;
        }

        public Builder cookiePath(String path) {
            this.cookiePath = requirePath(path);
            return this;
        }

        public Builder headerName(String name) {
            this.headerName = requireToken(name, "headerName");
            return this;
        }

        public Builder parameterName(String name) {
            this.parameterName = requireToken(name, "parameterName");
            return this;
        }

        public Builder tokenTtlSeconds(long ttl) {
            if (ttl <= 0 || ttl > 604800) {
                throw new IllegalArgumentException("TTL out of range");
            }
            this.tokenTtlSeconds = ttl;
            return this;
        }

        public Builder withPathExclusion(String p) {
            this.pathExclusions.add(requirePathPattern(p));
            return this;
        }

        public CsrfConfig build() {
            return new CsrfConfig(this);
        }
    }

    private static String requireToken(String v, String field) {
        Objects.requireNonNull(v, field);

        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if (!(c >= 'a' && c <= 'z') &&
                !(c >= 'A' && c <= 'Z') &&
                !(c >= '0' && c <= '9') &&
                c != '-' && c != '_' && c != '.') {
                throw new IllegalArgumentException(field + " invalid: " + c);
            }
        }
        return v;
    }

    private static String requirePath(String v) {
        Objects.requireNonNull(v);
        if (!v.isEmpty() && v.charAt(0) == '/') throw new IllegalArgumentException();

        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if (c < 0x20 || c > 0x7E || c == ';') {
                throw new IllegalArgumentException("Invalid path char: " + c);
            }
        }
        return v;
    }

    private static String requirePathPattern(String v) {
        Objects.requireNonNull(v);
        if (!v.isEmpty() && v.charAt(0) == '/') throw new IllegalArgumentException();
        if (v.contains("..")) throw new IllegalArgumentException();
        return v;
    }

    private static final byte[] EMPTY = new byte[0];

    static byte[] ascii(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }
}