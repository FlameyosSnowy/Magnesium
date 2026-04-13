package net.magnesiumbackend.core.security;

/**
 * Configuration for security headers added to HTTP responses.
 *
 * <p>SecurityHeadersConfig defines which security headers should be added
 * to outgoing HTTP responses. Use the builder or predefined presets to
 * configure headers appropriate for your deployment.</p>
 *
 * <h3>Supported Headers</h3>
 * <ul>
 *   <li>{@code X-Content-Type-Options} - Prevents MIME type sniffing</li>
 *   <li>{@code X-Frame-Options} - Clickjacking protection</li>
 *   <li>{@code X-XSS-Protection} - Legacy XSS filter</li>
 *   <li>{@code Strict-Transport-Security} - HTTPS enforcement (HSTS)</li>
 *   <li>{@code Content-Security-Policy} - Resource loading policy</li>
 *   <li>{@code Referrer-Policy} - Referrer information control</li>
 *   <li>{@code Permissions-Policy} - Browser feature restrictions</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // HTTPS deployment with strict headers
 * SecurityHeadersConfig config = SecurityHeadersConfig.httpsDefaults();
 *
 * // Custom configuration
 * SecurityHeadersConfig config = SecurityHeadersConfig.builder()
 *     .frameOptions("SAMEORIGIN")
 *     .contentSecurityPolicy("default-src 'self'; script-src 'self' 'unsafe-inline'")
 *     .build();
 * }</pre>
 *
 * @see SecurityHeadersFilter
 */
public final class SecurityHeadersConfig {
    private final boolean contentTypeOptions;
    private final boolean xssProtection;
    private final String frameOptions;       // DENY | SAMEORIGIN | null=disabled
    private final String hsts;              // null = disabled (non-HTTPS)
    private final String contentSecurityPolicy;
    private final String referrerPolicy;
    private final String permissionsPolicy;

    private SecurityHeadersConfig(Builder b) {
        this.contentTypeOptions     = b.contentTypeOptions;
        this.xssProtection          = b.xssProtection;
        this.frameOptions           = b.frameOptions;
        this.hsts                   = b.hsts;
        this.contentSecurityPolicy  = b.contentSecurityPolicy;
        this.referrerPolicy         = b.referrerPolicy;
        this.permissionsPolicy      = b.permissionsPolicy;
    }

    /**
     * Returns default configuration for HTTP deployments.
     * Excludes HSTS and CSP (which require HTTPS consideration).
     */
    public static SecurityHeadersConfig defaults() {
        return builder()
            .contentTypeOptions(true)
            .xssProtection(true)
            .frameOptions("DENY")
            .referrerPolicy("strict-origin-when-cross-origin")
            .build();
    }

    /**
     * Returns default configuration for HTTPS deployments.
     * Includes HSTS and CSP for maximum security.
     */
    public static SecurityHeadersConfig httpsDefaults() {
        return builder()
            .contentTypeOptions(true)
            .xssProtection(true)
            .frameOptions("DENY")
            .hsts("max-age=31536000; includeSubDomains")
            .referrerPolicy("strict-origin-when-cross-origin")
            .contentSecurityPolicy("default-src 'self'")
            .build();
    }

    /**
     * Returns configuration with all headers disabled.
     */
    public static SecurityHeadersConfig none() {
        return builder().build();
    }

    public boolean contentTypeOptions() {
        return contentTypeOptions;
    }

    public boolean xssProtection() {
        return xssProtection;
    }

    public String frameOptions() {
        return frameOptions;
    }

    public String hsts() {
        return hsts;
    }

    public String contentSecurityPolicy() {
        return contentSecurityPolicy;
    }

    public String referrerPolicy() {
        return referrerPolicy;
    }

    public String permissionsPolicy() {
        return permissionsPolicy;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private boolean contentTypeOptions = true;
        private boolean xssProtection = true;
        private String frameOptions = "DENY";
        private String hsts;
        private String contentSecurityPolicy;
        private String referrerPolicy;
        private String permissionsPolicy;

        public Builder contentTypeOptions(boolean v)        { this.contentTypeOptions = v; return this; }
        public Builder xssProtection(boolean v)             { this.xssProtection = v; return this; }
        public Builder frameOptions(String v)               { this.frameOptions = v; return this; }
        public Builder hsts(String v)                       { this.hsts = v; return this; }
        public Builder contentSecurityPolicy(String v)      { this.contentSecurityPolicy = v; return this; }
        public Builder referrerPolicy(String v)             { this.referrerPolicy = v; return this; }
        public Builder permissionsPolicy(String v)          { this.permissionsPolicy = v; return this; }
        public SecurityHeadersConfig build()                { return new SecurityHeadersConfig(this); }
    }
}