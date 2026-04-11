package net.magnesiumbackend.core.http;

import net.magnesiumbackend.core.security.RequestSigningFilter;
import net.magnesiumbackend.core.security.SecurityHeadersFilter;

public class RequestSecurityRegistryBuilder {
    private RequestSigningFilter filter;
    private SecurityHeadersFilter securityHeadersFilter;

    public RequestSecurityRegistryBuilder securityHeadersFilter(SecurityHeadersFilter securityHeadersFilter) {
        this.securityHeadersFilter = securityHeadersFilter;
        return this;
    }

    public RequestSecurityRegistryBuilder signingFilter(RequestSigningFilter filter) {
        this.filter = filter;
        return this;
    }

    public RequestSecurityRegistry build() {
        return new RequestSecurityRegistry(filter, securityHeadersFilter);
    }
}
