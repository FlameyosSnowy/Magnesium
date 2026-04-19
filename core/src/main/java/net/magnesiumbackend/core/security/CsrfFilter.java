package net.magnesiumbackend.core.security;

import net.magnesiumbackend.core.http.response.HttpMethod;
import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.route.FilterChain;
import net.magnesiumbackend.core.route.HttpFilter;
import net.magnesiumbackend.core.route.RequestContext;
import net.magnesiumbackend.core.security.exceptions.CsrfException;
import net.magnesiumbackend.core.utils.ByteBufBuilder;
import org.jetbrains.annotations.NotNull;

/**
 * HTTP filter that provides CSRF (Cross-Site Request Forgery) protection.
 *
 * <p>CsrfFilter validates that state-changing requests (POST, PUT, DELETE, PATCH)
 * include a valid CSRF token matching the token stored in the user's cookie. This
 * prevents attackers from forging requests from other sites.</p>
 *
 * <h3>How It Works</h3>
 * <ol>
 *   <li>On each request, the filter checks if a CSRF cookie exists</li>
 *   <li>If missing or expired, a new token is generated and set as a cookie</li>
 *   <li>For state-changing requests, the token must be sent back in either:
 *     <ul>
 *       <li>HTTP header (default: X-XSRF-TOKEN)</li>
 *       <li>Form parameter (default: _csrf)</li>
 *     </ul>
 *   </li>
 *   <li>If tokens don't match, a {@link CsrfException} is thrown (403 Forbidden)</li>
 * </ol>
 *
 * <h3>Safe Methods</h3>
 * <p>GET, HEAD, OPTIONS, and TRACE requests are considered "safe" and skip
 * token validation (but still receive the cookie token).</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * MagnesiumApplication.builder()
 *     .filter(new CsrfFilter(CsrfConfig.defaults()))
 *     .build();
 *
 * // Frontend JavaScript must read the cookie and send header:
 * // const token = getCookie('XSRF-TOKEN');
 * // fetch('/api/data', {
 * //   method: 'POST',
 * //   headers: { 'X-XSRF-TOKEN': token }
 * // });
 * }</pre>
 *
 * @see CsrfConfig
 * @see CsrfToken
 * @see CsrfException
 */
public final class CsrfFilter implements HttpFilter {

    private final CsrfConfig config;

    /**
     * Creates a CSRF filter with the given configuration.
     *
     * @param config CSRF protection configuration
     */
    public CsrfFilter(@NotNull CsrfConfig config) {
        this.config = config;
    }

    /**
     * Creates a CSRF filter with default configuration.
     *
     * @return default CSRF filter
     */
    public static CsrfFilter defaults() {
        return new CsrfFilter(CsrfConfig.defaults());
    }

    @Override
    public Object handle(RequestContext ctx, FilterChain chain) {
        String path = ctx.request().path();
        HttpMethod method = ctx.request().method();

        // Skip CSRF for excluded paths
        if (config.isPathExcluded(path)) {
            return chain.next(ctx);
        }

        // Get or create CSRF token cookie
        CsrfToken cookieToken = getOrCreateToken(ctx);

        // Validate token for state-changing methods
        if (!isSafeMethod(method)) {
            String requestToken = extractRequestToken(ctx);

            if (requestToken == null) {
                throw new CsrfException("CSRF token missing from request");
            }

            if (!cookieToken.value().equals(requestToken)) {
                throw new CsrfException("CSRF token mismatch");
            }
        }

        return chain.next(ctx);
    }

    /**
     * Gets the existing CSRF token from cookie or creates a new one.
     *
     * @param ctx request context
     * @return the CSRF token (from cookie or newly generated)
     */
    private CsrfToken getOrCreateToken(RequestContext ctx) {
        String existingToken = ctx.cookie(config.cookieName());

        if (existingToken != null && !existingToken.isEmpty()) {
            return CsrfToken.of(existingToken);
        }

        // Generate new token
        CsrfToken newToken = CsrfToken.generate(config.tokenTtlSeconds());

        // Set cookie - will be applied to response
        ctx.set(CsrfFilter.class.getName() + ".token", newToken);

        return newToken;
    }

    /**
     * Extracts CSRF token from request header or form parameter.
     *
     * @param ctx request context
     * @return the submitted token, or null if not found
     */
    private String extractRequestToken(RequestContext ctx) {
        // Try header first
        String headerToken = ctx.header(config.headerName());
        if (headerToken != null && !headerToken.isEmpty()) {
            return headerToken;
        }

        // Try form parameter
        return ctx.queryParam(config.parameterName());
    }

    /**
     * Checks if the HTTP method is "safe" (doesn't modify state).
     *
     * @param method HTTP method name
     * @return true if method is safe (GET, HEAD, OPTIONS, TRACE)
     */
    private boolean isSafeMethod(HttpMethod method) {
        return config.safeMethods().contains(method);
    }

    /**
     * Applies the CSRF cookie to the response.
     * This method should be called by the response processing pipeline.
     *
     * @param ctx request context
     * @param response the response being sent
     */
    public void applyCsrfCookie(RequestContext ctx, ResponseEntity<?> response) {
        CsrfToken token = ctx.get(CsrfFilter.class.getName() + ".token");
        if (token == null) return;

        String cookieName = config.cookieName();
        String value = token.value();
        String cookiePath = config.cookiePath();
        SameSite sameSite = config.sameSite();

        byte[] fragment = sameSite.fragment;

        try (ByteBufBuilder cookie = ByteBufBuilder.acquire(
            48 +
                cookieName.length() + value.length() +
                cookiePath.length() + fragment.length
        )) {
            cookie.appendAscii(cookieName);
            cookie.append('=');
            cookie.appendAscii(value);
            cookie.appendAscii("; Path=");
            cookie.appendAscii(cookiePath);
            cookie.appendAscii("; SameSite=");
            cookie.append(fragment);
            cookie.append(config.httpOnlyFragment());
            cookie.append(config.secureFragment());

            response.headers().set("Set-Cookie", cookie.build());
        }
    }
}
