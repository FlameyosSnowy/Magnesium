package net.magnesiumbackend.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Requires request signature verification for a route.
 *
 * <p>When applied, the framework verifies that the request contains a valid
 * cryptographic signature before processing. This is commonly used for:
 * <ul>
 *   <li>Webhook endpoints to verify sender authenticity</li>
 *   <li>API requests requiring HMAC or asymmetric signature verification</li>
 *   <li>Protection against request tampering</li>
 * </ul>
 * </p>
 *
 * <p>The signature algorithm and verification details are configured via
 * {@link net.magnesiumbackend.core.security.RequestSigningFilter} and the
 * application security settings.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @RestController
 * public class WebhookController {
 *     @PostMapping(path = "/webhooks/payment")
 *     @VerifySignature
 *     public ResponseEntity<Void> handlePaymentWebhook(@Body PaymentEvent event) {
 *         // Only processes if request signature is valid
 *         paymentService.process(event);
 *         return ResponseEntity.ok().build();
 *     }
 * }
 * }</pre>
 *
 * <p>When signature verification fails, the framework returns HTTP 401 Unauthorized.</p>
 *
 * @see net.magnesiumbackend.core.security.RequestSigningFilter
 * @see Authenticated
 * @see RateLimit
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface VerifySignature {
}
