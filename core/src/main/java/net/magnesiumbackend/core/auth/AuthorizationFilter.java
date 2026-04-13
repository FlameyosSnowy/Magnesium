package net.magnesiumbackend.core.auth;

import net.magnesiumbackend.core.annotations.enums.RequiresMode;
import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.route.FilterChain;
import net.magnesiumbackend.core.route.HttpFilter;
import net.magnesiumbackend.core.route.RequestContext;

/**
 * HTTP filter that enforces authorization rules on incoming requests.
 *
 * <p>The authorization filter runs after authentication and verifies that the
 * authenticated principal has the required permissions to access the resource.
 * It can require authentication and/or specific permissions.</p>
 *
 * <h3>Authorization Modes</h3>
 * <ul>
 *   <li>{@link RequiresMode#ALL} - Principal must have ALL listed permissions</li>
 *   <li>{@link RequiresMode#ANY} - Principal must have ANY ONE of the listed permissions</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Require authentication only
 * new AuthorizationFilter(true, new String[0], RequiresMode.ALL)
 *
 * // Require specific permission
 * new AuthorizationFilter(true, new String[]{"orders:read"}, RequiresMode.ALL)
 *
 * // Require any of multiple permissions
 * new AuthorizationFilter(true,
 *     new String[]{"admin", "moderator"},
 *     RequiresMode.ANY)
 * }</pre>
 *
 * <p>Usually applied via annotations ({@code @Authenticated}, {@code @Requires}, {@code @Secured})
 * rather than direct filter construction.</p>
 *
 * @see AuthenticationFilter
 * @see RequiresMode
 * @see HttpFilter
 * @see net.magnesiumbackend.core.annotations.Authenticated
 * @see net.magnesiumbackend.core.annotations.Requires
 */
public final class AuthorizationFilter implements HttpFilter {

    private final boolean      requiresAuthentication;
    private final String[]      permissions;
    private final RequiresMode  mode;

    /**
     * Creates a new authorization filter.
     *
     * @param requiresAuthentication if true, anonymous principals are rejected with 401
     * @param permissions          the required permissions (may be empty for auth-only)
     * @param mode                 how to evaluate multiple permissions (ALL or ANY)
     */
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