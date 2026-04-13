package net.magnesiumbackend.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applies rate limiting to a route or controller.
 *
 * <p>Limits the number of requests that can be made within a time window,
 * protecting endpoints from abuse, brute-force attacks, or accidental overload.</p>
 *
 * <h3>Algorithms</h3>
 * <ul>
 *   <li><b>FIXED_WINDOW</b> - Simple counter that resets at window boundaries.
 *       Allows bursts at window edges but is memory efficient.</li>
 *   <li><b>SLIDING_WINDOW</b> - Smooth rate limiting using a sliding time window.
 *       Prevents burst attacks at window edges. Default algorithm.</li>
 *   <li><b>TOKEN_BUCKET</b> - Allows bursts up to bucket capacity while maintaining
 *       average rate. Good for handling traffic spikes.</li>
 * </ul>
 *
 * <h3>Key Resolvers</h3>
 * <ul>
 *   <li><b>IP</b> - Rate limit by client IP address (default)</li>
 *   <li><b>USER</b> - Rate limit by authenticated user ID</li>
 *   <li><b>API_KEY</b> - Rate limit by API key from request header</li>
 * </ul>
 *
 * <p>Example usage on a method:</p>
 * <pre>{@code
 * @RestController
 * public class ApiController {
 *     @PostMapping(path = "/login")
 *     @RateLimit(requests = 5, windowSeconds = 60, algorithm = Algorithm.FIXED_WINDOW)
 *     public ResponseEntity<AuthToken> login(@Body LoginRequest request) {
 *         // Protected against brute force attacks
 *         return ResponseEntity.ok(authService.authenticate(request));
 *     }
 *
 *     @GetMapping(path = "/api/data")
 *     @RateLimit(requests = 1000, windowSeconds = 3600, keyResolver = KeyResolverType.API_KEY)
 *     public ResponseEntity<Data> getData() {
 *         // 1000 requests per hour per API key
 *         return ResponseEntity.ok(dataService.fetch());
 *     }
 * }
 * }</pre>
 *
 * <p>When the limit is exceeded, the framework returns HTTP 429 Too Many Requests.</p>
 *
 * @see Authenticated
 * @see Anonymous
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
public @interface RateLimit {
    /**
     * Maximum number of requests allowed in the time window.
     *
     * @return the request limit (default: 100)
     */
    int     requests()      default 100;

    /**
     * Time window duration in seconds.
     *
     * @return the window duration in seconds (default: 60)
     */
    long    windowSeconds() default 60;

    /**
     * Rate limiting algorithm to use.
     *
     * @return the algorithm (default: SLIDING_WINDOW)
     */
    Algorithm algorithm()   default Algorithm.SLIDING_WINDOW;

    /**
     * How to identify clients for rate limiting purposes.
     *
     * @return the key resolver type (default: IP)
     */
    KeyResolverType keyResolver() default KeyResolverType.IP;

    /**
     * Rate limiting algorithms.
     */
    enum Algorithm {
        /** Simple fixed window counter. */
        FIXED_WINDOW,
        /** Sliding window for smooth limiting. */
        SLIDING_WINDOW,
        /** Token bucket for burst handling. */
        TOKEN_BUCKET
    }

    /**
     * Strategies for identifying the client being rate limited.
     */
    enum KeyResolverType {
        /** Rate limit by IP address. */
        IP,
        /** Rate limit by authenticated user. */
        USER,
        /** Rate limit by API key. */
        API_KEY
    }
}