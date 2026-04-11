package net.magnesiumbackend.core.auth;

import net.magnesiumbackend.core.annotations.enums.RequiresMode;
import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.route.FilterChain;
import net.magnesiumbackend.core.route.HttpFilter;
import net.magnesiumbackend.core.route.RequestContext;

public final class AuthorizationFilter implements HttpFilter {

    private final boolean      requiresAuthentication;
    private final String[]      permissions;
    private final RequiresMode  mode;

    public AuthorizationFilter(
        boolean requiresAuthentication,
        String[] permissions,
        RequiresMode mode
    ) {
        this.requiresAuthentication = requiresAuthentication;
        this.permissions            = permissions;
        this.mode                   = mode;
    }

    @Override
    public ResponseEntity<?> handle(RequestContext ctx, FilterChain chain) {
        Principal principal = ctx.principal();

        if (requiresAuthentication && principal.isAnonymous()) {
            return ResponseEntity.of(401, "Authentication required");
        }

        if (permissions != null && permissions.length > 0) {
            boolean granted = switch (mode) {
                case ALL -> {
                    boolean b = true;
                    for (String permission : permissions) {
                        if (!principal.hasPermission(permission)) {
                            b = false;
                            break;
                        }
                    }
                    yield b;
                }
                case ANY -> {
                    boolean b = false;
                    for (String permission : permissions) {
                        if (principal.hasPermission(permission)) {
                            b = true;
                            break;
                        }
                    }
                    yield b;
                }
            };
            if (!granted) {
                return ResponseEntity.of(403, "Insufficient permissions");
            }
        }

        return chain.next(ctx);
    }
}