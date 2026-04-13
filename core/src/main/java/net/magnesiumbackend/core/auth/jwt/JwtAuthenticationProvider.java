package net.magnesiumbackend.core.auth.jwt;

import net.magnesiumbackend.core.auth.*;
import net.magnesiumbackend.core.auth.exceptions.AuthenticationException;
import net.magnesiumbackend.core.route.RequestContext;
import net.magnesiumbackend.core.headers.Slice;

import java.util.Optional;

/**
 * JWT Bearer token authentication provider.
 *
 * <p>Extracts and validates JWT tokens from the {@code Authorization} header
 * using the Bearer scheme. The token is verified using the configured
 * {@link NimbusJwtVerifier}.</p>
 *
 * <h3>Expected Header Format</h3>
 * <pre>Authorization: Bearer &lt;jwt-token&gt;</pre>
 *
 * <h3>Principal Mapping</h3>
 * <ul>
 *   <li>userId: {@code sub} claim from JWT</li>
 *   <li>username: {@code username} claim, or {@code sub} if username not present</li>
 *   <li>permissions: {@code permissions} claim (string array)</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // HMAC-SHA256 with shared secret
 * byte[] secret = loadSecretFromEnv();
 * NimbusJwtVerifier verifier = new NimbusJwtVerifier(secret);
 * JwtAuthenticationProvider provider = new JwtAuthenticationProvider(verifier);
 * }</pre>
 *
 * @see NimbusJwtVerifier
 * @see AuthenticationProvider
 */
public final class JwtAuthenticationProvider implements AuthenticationProvider {

    private final NimbusJwtVerifier verifier;

    /**
     * Creates a new JWT authentication provider.
     *
     * @param verifier the JWT verifier for token validation
     */
    public JwtAuthenticationProvider(NimbusJwtVerifier verifier) {
        this.verifier = verifier;
    }

    @Override
    public Optional<Principal> authenticate(RequestContext ctx) throws AuthenticationException {
        Slice header = ctx.headerRaw("Authorization");

        if (header == null || header.length() < 8) {
            return Optional.empty();
        }

        if (!startsWithBearer(header)) {
            return Optional.empty();
        }

        Slice tokenSlice = trim(header.slice(7));
        if (tokenSlice.length() == 0) {
            return Optional.empty();
        }

        String token = tokenSlice.materialize();
        var claims = verifier.verify(token);

        return Optional.of(Principal.of(
            claims.getSubject(),
            getUsername(claims),
            NimbusJwtVerifier.extractPermissions(claims)
        ));
    }

    private static boolean startsWithBearer(Slice s) {
        if (s.length() < 7) return false;

        return  (s.charAt(0) == 'B' || s.charAt(0) == 'b') &&
            (s.charAt(1) == 'e' || s.charAt(1) == 'E') &&
            (s.charAt(2) == 'a' || s.charAt(2) == 'A') &&
            (s.charAt(3) == 'r' || s.charAt(3) == 'R') &&
            (s.charAt(4) == 'e' || s.charAt(4) == 'E') &&
            (s.charAt(5) == 'r' || s.charAt(5) == 'R') &&
            s.charAt(6) == ' ';
    }

    private static Slice trim(Slice s) {
        int start = 0;
        int end = s.length();

        while (start < end && isSpace(s.charAt(start))) start++;
        while (end > start && isSpace(s.charAt(end - 1))) end--;

        return s.slice(start, end);
    }

    private static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    private static String getUsername(com.nimbusds.jwt.JWTClaimsSet claims) {
        try {
            String username = claims.getStringClaim("username");
            return username != null ? username : claims.getSubject();
        } catch (Exception e) {
            return claims.getSubject();
        }
    }
}