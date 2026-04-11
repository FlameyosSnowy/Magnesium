package net.magnesiumbackend.core.auth.jwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import net.magnesiumbackend.core.auth.exceptions.AuthenticationException;

import java.util.List;
import java.util.Set;

public final class NimbusJwtVerifier {

    private final ConfigurableJWTProcessor<SecurityContext> processor;

    /** HMAC-SHA256 (HS256), use this for shared-secret setups */
    public NimbusJwtVerifier(byte[] sharedSecret) {
        this.processor = new DefaultJWTProcessor<>();
        var key = new ImmutableSecret<>(sharedSecret);
        var selector = new JWSVerificationKeySelector<>(JWSAlgorithm.HS256, key);
        processor.setJWSKeySelector(selector);
    }

    /** RSA/EC (RS256, ES256, etc.), use this for public/private key setups */
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