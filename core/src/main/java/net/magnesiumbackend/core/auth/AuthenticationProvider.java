package net.magnesiumbackend.core.auth;

import net.magnesiumbackend.core.auth.exceptions.AuthenticationException;
import net.magnesiumbackend.core.route.RequestContext;
import java.util.Optional;

/**
 * Strategy interface for extracting and validating user identity from HTTP requests.
 *
 * <p>Authentication providers are the entry point for all authentication in Magnesium.
 * Each provider attempts to extract credentials from the request (JWT tokens, session
 * cookies, API keys, etc.) and convert them into a {@link Principal}.</p>
 *
 * <h3>Provider Chain</h3>
 * <p>Multiple providers can be registered with the {@link AuthenticationFilter}. They are
 * tried in order until one returns a non-empty Principal. If all providers return empty,
 * the request proceeds with an {@link Principal#anonymous()} principal.</p>
 *
 * <h3>Return Values</h3>
 * <ul>
 *   <li><b>Present Principal</b> - Authentication succeeded, request proceeds with this identity</li>
 *   <li><b>Empty Optional</b> - This provider doesn't apply (e.g., no Authorization header), try next provider</li>
 *   <li><b>AuthenticationException</b> - Credentials present but invalid, request fails with 401</li>
 * </ul>
 *
 * <p>Example JWT implementation:</p>
 * <pre>{@code
 * public class JwtAuthProvider implements AuthenticationProvider {
 *     private final JwtVerifier verifier;
 *
 *     public JwtAuthProvider(JwtVerifier verifier) {
 *         this.verifier = verifier;
 *     }
 *
 *     @Override
 *     public Optional<Principal> authenticate(RequestContext ctx) {
 *         String authHeader = ctx.header("Authorization");
 *         if (authHeader == null || !authHeader.startsWith("Bearer ")) {
 *             return Optional.empty(); // Not a JWT request, try next provider
 *         }
 *
 *         String token = authHeader.substring(7);
 *         try {
 *             JwtClaims claims = verifier.verify(token);
 *             Principal principal = Principal.of(
 *                 claims.getSubject(),
 *                 claims.getUsername(),
 *                 claims.getPermissions()
 *             );
 *             return Optional.of(principal);
 *         } catch (JwtException e) {
 *             throw new AuthenticationException("Invalid token: " + e.getMessage());
 *         }
 *     }
 * }
 * }</pre>
 *
 * @see AuthenticationFilter
 * @see Principal
 * @see AuthenticationException
 */
public interface AuthenticationProvider {
    /**
     * Attempts to extract and validate a Principal from the request.
     *
     * <p>Returns {@link Optional#empty()} if this provider doesn't apply to the request
     * (e.g., missing Authorization header, wrong authentication scheme). This allows
     * the next provider in the chain to attempt authentication.</p>
     *
     * <p>Throws {@link AuthenticationException} if credentials are present but invalid.
     * This immediately fails the request with HTTP 401 Unauthorized.</p>
     *
     * @param ctx the request context containing headers, cookies, and request metadata
     * @return the authenticated principal, or empty if this provider doesn't apply
     * @throws AuthenticationException if credentials are present but invalid
     */
    Optional<Principal> authenticate(RequestContext ctx) throws AuthenticationException;
}