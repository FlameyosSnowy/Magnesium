package net.magnesiumbackend.core.auth.jwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import net.magnesiumbackend.core.auth.Principal;
import net.magnesiumbackend.core.auth.exceptions.AuthenticationException;

import java.util.List;
import java.util.Set;

/**
 * JWT token verifier using the Nimbus JOSE+JWT library.
 *
 * <p>Supports both HMAC (shared secret) and asymmetric (RSA/EC) signature algorithms.
 * The verifier extracts claims from valid tokens for use in {@link Principal} creation.</p>
 *
 * <h3>Supported Algorithms</h3>
 * <ul>
 *   <li><b>HS256</b> - HMAC with SHA-256 (shared secret)</li>
 *   <li><b>RS256</b> - RSA with SHA-256 (public/private key)</li>
 *   <li><b>ES256</b> - ECDSA with SHA-256 (elliptic curve)</li>
 * </ul>
 *
 * <h3>Expected Token Claims</h3>
 * <ul>
 *   <li>{@code sub} - Subject identifier (becomes userId)</li>
 *   <li>{@code username} - Human-readable username (optional)</li>
 *   <li>{@code permissions} - Array of permission strings (optional)</li>
 * </ul>
 *
 * <p>Example HMAC setup:</p>
 * <pre>{@code
 * byte[] secret = System.getenv("JWT_SECRET").getBytes();
 * NimbusJwtVerifier verifier = new NimbusJwtVerifier(secret);
 * }</pre>
 *
 * <p>Example RSA setup:</p>
 * <pre>{@code
 * JWKSource<SecurityContext> jwkSource = loadPublicKeys();
 * NimbusJwtVerifier verifier = new NimbusJwtVerifier(jwkSource, JWSAlgorithm.RS256);
 * }</pre>
 *
 * @see JwtAuthenticationProvider
 * @see <a href="https://connect2id.com/products/nimbus-jose-jwt">Nimbus JOSE+JWT</a>
 */
public final class NimbusJwtVerifier {

    private final ConfigurableJWTProcessor<SecurityContext> processor;

    /**
     * Creates a verifier for HMAC-SHA256 (HS256) shared-secret tokens.
     *
     * <p>Use this constructor for simple setups where the same secret is shared
     * between token issuer and verifier.</p>
     *
     * @param sharedSecret the HMAC secret key bytes
     */
    public NimbusJwtVerifier(byte[] sharedSecret) {
        this.processor = new DefaultJWTProcessor<>();
        var key = new ImmutableSecret<>(sharedSecret);
        var selector = new JWSVerificationKeySelector<>(JWSAlgorithm.HS256, key);
        processor.setJWSKeySelector(selector);
    }

    /**
     * Creates a verifier for asymmetric signature algorithms (RSA, EC).
     *
     * <p>Use this constructor for public/private key setups where the verifier
     * only needs access to public keys.</p>
     *
     * @param jwkSource the source of public keys for verification
     * @param algorithm the expected signature algorithm (RS256, ES256, etc.)
     */
    public NimbusJwtVerifier(com.nimbusds.jose.jwk.source.JWKSource<SecurityContext> jwkSource,
                             JWSAlgorithm algorithm) {
        this.processor = new DefaultJWTProcessor<>();
        processor.setJWSKeySelector(new JWSVerificationKeySelector<>(algorithm, jwkSource));
    }

    public JWTClaimsSet verify(String token) throws AuthenticationException {
        try {
            return processor.process(token, null);
        } catch (Exception e) {
            throw new AuthenticationException("JWT verification failed: " + e.getMessage(), e);
        }
    }

    public static Set<String> extractPermissions(JWTClaimsSet claims) {
        try {
            // expects a "permissions" claim: ["orders:read", "orders:write"]
            List<String> perms = claims.getStringListClaim("permissions");
            return perms != null ? Set.copyOf(perms) : Set.of();
        } catch (Exception e) {
            return Set.of();
        }
    }
}