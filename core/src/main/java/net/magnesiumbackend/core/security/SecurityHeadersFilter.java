package net.magnesiumbackend.core.security;

import net.magnesiumbackend.core.headers.HttpHeaderIndex;
import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.route.FilterChain;
import net.magnesiumbackend.core.route.HttpFilter;
import net.magnesiumbackend.core.route.RequestContext;

import java.util.Map;

/**
 * HTTP filter that adds security headers to responses.
 *
 * <p>SecurityHeadersFilter applies configured security headers to all outgoing
 * HTTP responses. The filter passes through the request normally and adds
 * headers via the {@link #applyTo(ResponseEntity)} method, which should be
 * called by the response processing pipeline.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * MagnesiumApplication.builder()
 *     .filter(new SecurityHeadersFilter(SecurityHeadersConfig.httpsDefaults()))
 *     .build();
 * }</pre>
 *
 * @see SecurityHeadersConfig
 * @see HttpFilter
 */
public final class SecurityHeadersFilter implements HttpFilter {
    private final SecurityHeadersConfig config;

    /**
     * Creates a filter with the given configuration.
     *
     * @param config the security headers configuration
     */
    public SecurityHeadersFilter(SecurityHeadersConfig config) {
        this.config = config;
    }

    /**
     * Creates a filter with default security headers.
     *
     * @return a filter with {@link SecurityHeadersConfig#defaults()}
     */
    public static SecurityHeadersFilter defaults() {
        return new SecurityHeadersFilter(SecurityHeadersConfig.defaults());
    }

    @Override
    public ResponseEntity<?> handle(RequestContext ctx, FilterChain chain) {
        return chain.next(ctx);
    }

    public void applyTo(ResponseEntity<?> response) {
        HttpHeaderIndex headers = response.headers();

        if (config.contentTypeOptions())
            headers.set("X-Content-Type-Options", "nosniff");

        if (config.frameOptions() != null)
            headers.set("X-Frame-Options", config.frameOptions());

        if (config.xssProtection())
            headers.set("X-XSS-Protection", "1; mode=block");

        if (config.hsts() != null)
            headers.set("Strict-Transport-Security", config.hsts());

        if (config.contentSecurityPolicy() != null)
            headers.set("Content-Security-Policy", config.contentSecurityPolicy());

        if (config.referrerPolicy() != null)
            headers.set("Referrer-Policy", config.referrerPolicy());

        if (config.permissionsPolicy() != null)
            headers.set("Permissions-Policy", config.permissionsPolicy());
    }
}