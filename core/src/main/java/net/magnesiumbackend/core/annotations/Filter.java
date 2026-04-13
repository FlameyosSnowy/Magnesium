package net.magnesiumbackend.core.annotations;

import net.magnesiumbackend.core.route.HttpFilter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applies one or more HTTP filters to a route or controller.
 *
 * <p>Filters intercept requests before they reach the route handler, enabling
 * cross-cutting concerns like logging, authentication, rate limiting, CORS,
 * request modification, or validation.</p>
 *
 * <p>Filters are executed in declaration order. Multiple filters can be
 * applied by repeating this annotation or using {@link Filters}.</p>
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>Filter's {@code handle} method is called with the request</li>
 *   <li>Filter can modify the request or short-circuit with a response</li>
 *   <li>If filter returns {@code null}, the next filter or route handler runs</li>
 * </ol>
 *
 * <p>Example usage on a method:</p>
 * <pre>{@code
 * @RestController
 * public class ApiController {
 *     @GetMapping(path = "/admin/logs")
 *     @Filter(AdminAuthFilter.class)
 *     @Filter(AuditLogFilter.class)
 *     public ResponseEntity<List<Log>> getLogs() {
 *         return ResponseEntity.ok(logService.findAll());
 *     }
 * }
 * }</pre>
 *
 * <p>Example usage on a controller (applies to all routes):</p>
 * <pre>{@code
 * @RestController
 * @Filter(RequestIdFilter.class)
 * @Filter(TimingFilter.class)
 * public class MonitoredController {
 *     // All routes have request ID and timing filters applied
 * }
 * }</pre>
 *
 * @see Filters
 * @see HttpFilter
 * @see net.magnesiumbackend.core.route.HttpFilter
 */
@Target({ ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Filters.class)
public @interface Filter {
    /**
     * The filter class to apply.
     *
     * <p>The filter class must implement {@link HttpFilter} and have a public
     * no-argument constructor, or be registered as a service for injection.</p>
     *
     * @return the filter class
     */
    Class<? extends HttpFilter> value();
}