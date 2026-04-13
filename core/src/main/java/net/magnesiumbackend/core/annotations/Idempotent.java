package net.magnesiumbackend.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a route or controller as idempotent, enabling response caching for
 * duplicate requests.
 *
 * <p>Idempotent requests can be safely retried without side effects. When
 * annotated, the framework caches successful responses and returns them for
 * subsequent identical requests within the TTL period.</p>
 *
 * <p>This is particularly useful for:
 * <ul>
 *   <li>Safe retries in distributed systems</li>
 *   <li>Deduplication of client retries</li>
 *   <li>Optimizing GET requests that are expensive to compute</li>
 * </ul>
 * </p>
 *
 * <p>Example usage on a method:</p>
 * <pre>{@code
 * @RestController
 * public class ReportController {
 *     @GetMapping(path = "/reports/expensive")
 *     @Idempotent(ttlHours = 2)
 *     public ResponseEntity<Report> generateExpensiveReport() {
 *         // Computationally expensive operation
 *         return ResponseEntity.ok(reportService.generate());
 *     }
 * }
 * }</pre>
 *
 * <p>Example usage on a controller (applies to all routes):</p>
 * <pre>{@code
 * @RestController
 * @Idempotent(ttlHours = 1)
 * public class CachedController {
 *     // All routes in this controller are idempotent-cached
 * }
 * }</pre>
 *
 * @see GetMapping
 * @see PostMapping
 * @see RestController
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
public @interface Idempotent {
    /**
     * Time-to-live for cached responses in hours.
     *
     * <p>After this period, duplicate requests will trigger fresh processing.
     * Defaults to 24 hours.</p>
     *
     * @return the cache TTL in hours
     */
    long ttlHours() default 24;
}