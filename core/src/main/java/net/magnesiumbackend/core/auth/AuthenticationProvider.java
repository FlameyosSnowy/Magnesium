package net.magnesiumbackend.core.auth;

import net.magnesiumbackend.core.auth.exceptions.AuthenticationException;
import net.magnesiumbackend.core.route.RequestContext;
import java.util.Optional;

public interface AuthenticationProvider {
    /**
     * Try to extract and validate a Principal from the request.
     * Return empty if this provider doesn't apply (wrong scheme, no cookie, etc.).
     * Throw AuthenticationException if the token is present but invalid.
     */
    Optional<Principal> authenticate(RequestContext ctx) throws AuthenticationException;
}