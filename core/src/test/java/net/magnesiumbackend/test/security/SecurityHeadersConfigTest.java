package net.magnesiumbackend.test.security;

import net.magnesiumbackend.core.security.SecurityHeadersConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SecurityHeadersConfig.
 */
class SecurityHeadersConfigTest {

    @Test
    void builderCreatesConfig() {
        SecurityHeadersConfig config = SecurityHeadersConfig.builder().build();
        assertNotNull(config);
    }

    @Test
    void defaultsConfiguration() {
        SecurityHeadersConfig config = SecurityHeadersConfig.defaults();

        assertTrue(config.contentTypeOptions());
        assertTrue(config.xssProtection());
        assertEquals("DENY", config.frameOptions());
        assertEquals("strict-origin-when-cross-origin", config.referrerPolicy());
        assertNull(config.hsts()); // HTTP defaults exclude HSTS
        assertNull(config.contentSecurityPolicy()); // HTTP defaults exclude CSP
        assertNull(config.permissionsPolicy()); // HTTP defaults exclude permissions
    }

    @Test
    void httpsDefaultsConfiguration() {
        SecurityHeadersConfig config = SecurityHeadersConfig.httpsDefaults();

        assertTrue(config.contentTypeOptions());
        assertTrue(config.xssProtection());
        assertEquals("DENY", config.frameOptions());
        assertEquals("strict-origin-when-cross-origin", config.referrerPolicy());
        assertEquals("max-age=31536000; includeSubDomains", config.hsts());
        assertEquals("default-src 'self'", config.contentSecurityPolicy());
    }

    @Test
    void noneConfiguration() {
        SecurityHeadersConfig config = SecurityHeadersConfig.none();

        assertTrue(config.contentTypeOptions()); // Builder defaults
        assertTrue(config.xssProtection());
        assertEquals("DENY", config.frameOptions());
    }

    @Test
    void customContentTypeOptions() {
        SecurityHeadersConfig config = SecurityHeadersConfig.builder()
            .contentTypeOptions(false)
            .build();

        assertFalse(config.contentTypeOptions());
    }

    @Test
    void customXssProtection() {
        SecurityHeadersConfig config = SecurityHeadersConfig.builder()
            .xssProtection(false)
            .build();

        assertFalse(config.xssProtection());
    }

    @Test
    void customFrameOptions() {
        SecurityHeadersConfig config = SecurityHeadersConfig.builder()
            .frameOptions("SAMEORIGIN")
            .build();

        assertEquals("SAMEORIGIN", config.frameOptions());
    }

    @Test
    void customHsts() {
        SecurityHeadersConfig config = SecurityHeadersConfig.builder()
            .hsts("max-age=86400")
            .build();

        assertEquals("max-age=86400", config.hsts());
    }

    @Test
    void customContentSecurityPolicy() {
        SecurityHeadersConfig config = SecurityHeadersConfig.builder()
            .contentSecurityPolicy("default-src 'self'; script-src 'self' 'unsafe-inline'")
            .build();

        assertEquals("default-src 'self'; script-src 'self' 'unsafe-inline'", config.contentSecurityPolicy());
    }

    @Test
    void customReferrerPolicy() {
        SecurityHeadersConfig config = SecurityHeadersConfig.builder()
            .referrerPolicy("no-referrer")
            .build();

        assertEquals("no-referrer", config.referrerPolicy());
    }

    @Test
    void customPermissionsPolicy() {
        SecurityHeadersConfig config = SecurityHeadersConfig.builder()
            .permissionsPolicy("camera=(), microphone=(), geolocation=(self)")
            .build();

        assertEquals("camera=(), microphone=(), geolocation=(self)", config.permissionsPolicy());
    }

    @Test
    void nullValuesForDisabled() {
        SecurityHeadersConfig config = SecurityHeadersConfig.builder()
            .frameOptions(null)
            .hsts(null)
            .contentSecurityPolicy(null)
            .referrerPolicy(null)
            .permissionsPolicy(null)
            .build();

        assertNull(config.frameOptions());
        assertNull(config.hsts());
        assertNull(config.contentSecurityPolicy());
        assertNull(config.referrerPolicy());
        assertNull(config.permissionsPolicy());
    }

    @Test
    void builderChaining() {
        SecurityHeadersConfig config = SecurityHeadersConfig.builder()
            .contentTypeOptions(true)
            .xssProtection(true)
            .frameOptions("SAMEORIGIN")
            .hsts("max-age=3600")
            .contentSecurityPolicy("default-src 'self'")
            .referrerPolicy("origin")
            .permissionsPolicy("camera=()")
            .build();

        assertNotNull(config);
        assertTrue(config.contentTypeOptions());
        assertEquals("SAMEORIGIN", config.frameOptions());
    }
}
