package net.magnesiumbackend.core.security;

import net.magnesiumbackend.core.http.ResponseEntity;
import net.magnesiumbackend.core.route.FilterChain;
import net.magnesiumbackend.core.route.HttpFilter;
import net.magnesiumbackend.core.route.RequestContext;

import java.util.Map;

public final class SecurityHeadersFilter implements HttpFilter {
    private final SecurityHeadersConfig config;

    public SecurityHeadersFilter(SecurityHeadersConfig config) {
        this.config = config;
    }

    public static SecurityHeadersFilter defaults() {
        return new SecurityHeadersFilter(SecurityHeadersConfig.defaults());
    }

    @Override
    public ResponseEntity<?> handle(RequestContext ctx, FilterChain chain) {
        return chain.next(ctx);
    }

    public void applyTo(ResponseEntity<?> response) {
        Map<String, String> headers = response.headers();

        if (config.contentTypeOptions())
            headers.put("X-Content-Type-Options", "nosniff");

        if (config.frameOptions() != null)
            headers.put("X-Frame-Options", config.frameOptions());

        if (config.xssProtection())
            headers.put("X-XSS-Protection", "1; mode=block");

        if (config.hsts() != null)
            headers.put("Strict-Transport-Security", config.hsts());

        if (config.contentSecurityPolicy() != null)
            headers.put("Content-Security-Policy", config.contentSecurityPolicy());

        if (config.referrerPolicy() != null)
            headers.put("Referrer-Policy", config.referrerPolicy());

        if (config.permissionsPolicy() != null)
            headers.put("Permissions-Policy", config.permissionsPolicy());
    }
}