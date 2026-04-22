package net.magnesiumbackend.test.security;

import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.route.FilterChain;
import net.magnesiumbackend.core.route.RequestContext;
import net.magnesiumbackend.core.security.SecurityHeadersConfig;
import net.magnesiumbackend.core.security.SecurityHeadersFilter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for SecurityHeadersFilter.
 */
class SecurityHeadersFilterTest {

    @Test
    void defaultsCreatesFilter() {
        SecurityHeadersFilter filter = SecurityHeadersFilter.defaults();
        assertNotNull(filter);
    }

    @Test
    void handlePassesThrough() {
        SecurityHeadersFilter filter = SecurityHeadersFilter.defaults();
        FilterChain chain = mock(FilterChain.class);
        RequestContext ctx = mock(RequestContext.class);
        Object expected = "result";

        when(chain.next(ctx)).thenReturn(expected);

        Object result = filter.handle(ctx, chain);

        assertEquals(expected, result);
        verify(chain).next(ctx);
    }

    @Test
    void applyToAddsContentTypeOptions() {
        SecurityHeadersConfig config = SecurityHeadersConfig.builder()
            .contentTypeOptions(true)
            .build();
        SecurityHeadersFilter filter = new SecurityHeadersFilter(config);

        ResponseEntity<?> response = ResponseEntity.ok("test");
        filter.applyTo(response);

        assertEquals("nosniff", response.headers().get("X-Content-Type-Options").materialize());
    }

    @Test
    void applyToAddsFrameOptions() {
        SecurityHeadersConfig config = SecurityHeadersConfig.builder()
            .frameOptions("DENY")
            .build();
        SecurityHeadersFilter filter = new SecurityHeadersFilter(config);

        ResponseEntity<?> response = ResponseEntity.ok("test");
        filter.applyTo(response);

        assertEquals("DENY", response.headers().get("X-Frame-Options").materialize());
    }

    @Test
    void applyToAddsXssProtection() {
        SecurityHeadersConfig config = SecurityHeadersConfig.builder()
            .xssProtection(true)
            .build();
        SecurityHeadersFilter filter = new SecurityHeadersFilter(config);

        ResponseEntity<?> response = ResponseEntity.ok("test");
        filter.applyTo(response);

        assertEquals("1; mode=block", response.headers().get("X-XSS-Protection").materialize());
    }

    @Test
    void applyToAddsHsts() {
        SecurityHeadersConfig config = SecurityHeadersConfig.builder()
            .hsts("max-age=31536000; includeSubDomains")
            .build();
        SecurityHeadersFilter filter = new SecurityHeadersFilter(config);

        ResponseEntity<?> response = ResponseEntity.ok("test");
        filter.applyTo(response);

        assertEquals("max-age=31536000; includeSubDomains", response.headers().get("Strict-Transport-Security").materialize());
    }

    @Test
    void applyToAddsCsp() {
        SecurityHeadersConfig config = SecurityHeadersConfig.builder()
            .contentSecurityPolicy("default-src 'self'")
            .build();
        SecurityHeadersFilter filter = new SecurityHeadersFilter(config);

        ResponseEntity<?> response = ResponseEntity.ok("test");
        filter.applyTo(response);

        assertEquals("default-src 'self'", response.headers().get("Content-Security-Policy").materialize());
    }

    @Test
    void applyToAddsReferrerPolicy() {
        SecurityHeadersConfig config = SecurityHeadersConfig.builder()
            .referrerPolicy("strict-origin-when-cross-origin")
            .build();
        SecurityHeadersFilter filter = new SecurityHeadersFilter(config);

        ResponseEntity<?> response = ResponseEntity.ok("test");
        filter.applyTo(response);

        assertEquals("strict-origin-when-cross-origin", response.headers().get("Referrer-Policy").materialize());
    }

    @Test
    void applyToAddsPermissionsPolicy() {
        SecurityHeadersConfig config = SecurityHeadersConfig.builder()
            .permissionsPolicy("camera=(), microphone=()")
            .build();
        SecurityHeadersFilter filter = new SecurityHeadersFilter(config);

        ResponseEntity<?> response = ResponseEntity.ok("test");
        filter.applyTo(response);

        assertEquals("camera=(), microphone=()", response.headers().get("Permissions-Policy").materialize());
    }
}
