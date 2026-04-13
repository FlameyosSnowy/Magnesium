package net.magnesiumbackend.core.auth;

import net.magnesiumbackend.core.auth.exceptions.AuthenticationException;
import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.route.*;
import java.util.List;

/**
 * HTTP filter that authenticates incoming requests using a chain of {@link AuthenticationProvider}s.
 *
 * <p>The authentication filter runs early in the request pipeline (before authorization) and
 * establishes the identity for the request. It tries each configured provider in order until
 * one successfully authenticates the request.</p>
 *
 * <h3>Authentication Flow</h3>
 * <ol>
 *   <li>Each provider attempts to extract credentials from the request</li>
 *   <li>First provider returning a {@link Principal} wins, principal is stored in context</li>
 *   <li>If a provider throws {@link AuthenticationException}, request fails with 401</li>
 *   <li>If all providers return empty, request proceeds with {@link Principal#anonymous()}</li>
 * </ol>
 *
 * <p>Example registration:</p>
 * <pre>{@code
 * MagnesiumApplication.builder()
 *     .http(http -> http
 *         .filter(new AuthenticationFilter(List.of(
 *             new JwtAuthenticationProvider(jwtVerifier),      // Try JWT first
 *             new CookieSessionAuthenticationProvider(store) // Fall back to session
 *         )))
 *     )
 *     .build();
 * }</pre>
 *
 * @see AuthenticationProvider
 * @see Principal
 * @see HttpFilter
 * @see AuthorizationFilter
 */
public final class AuthenticationFilter implements HttpFilter {

    private final List<AuthenticationProvider> providers;

    /**
     * Creates a new authentication filter with the given providers.
     *
     * <p>Providers are tried in list order. The first successful authentication wins.
     * An empty list means all requests proceed as anonymous.</p>
     *
     * @param providers the chain of authentication providers to try
     */
    public AuthenticationFilter(List<AuthenticationProvider> providers) {
        this.providers = providers;
    }

    @Override
    public ResponseEntity<?> handle(RequestContext ctx, FilterChain chain) {
        for (AuthenticationProvider provider : providers) {
            try {
                var principal = provider.authenticate(ctx);
                if (principal.isPresent()) {
                    ctx.setPrincipal(principal.get());
                    return chain.next(ctx);
                }
            } catch (AuthenticationException e) {
                return ResponseEntity.of(401, e.getMessage());
            }
        }

        ctx.setPrincipal(Principal.anonymous());
        return chain.next(ctx);
    }
}