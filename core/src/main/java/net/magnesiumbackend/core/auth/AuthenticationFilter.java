package net.magnesiumbackend.core.auth;

import net.magnesiumbackend.core.auth.exceptions.AuthenticationException;
import net.magnesiumbackend.core.http.ResponseEntity;
import net.magnesiumbackend.core.route.*;
import java.util.List;

public final class AuthenticationFilter implements HttpFilter {

    private final List<AuthenticationProvider> providers;

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