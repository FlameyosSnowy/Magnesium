package net.magnesiumbackend.core.http;

import net.magnesiumbackend.core.security.RequestSigningFilter;
import net.magnesiumbackend.core.security.SecurityHeadersFilter;

public record RequestSecurityRegistry(RequestSigningFilter signingFilter, SecurityHeadersFilter securityHeadersFilter) {
}
