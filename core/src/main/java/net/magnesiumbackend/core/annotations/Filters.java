package net.magnesiumbackend.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation for multiple {@link Filter} annotations.
 *
 * <p>Used implicitly when multiple {@code @Filter} annotations are declared
 * on the same element. Can also be used explicitly to group filters.</p>
 *
 * <p>Example explicit usage:</p>
 * <pre>{@code
 * @RestController
 * @Filters({
 *     @Filter(AuthFilter.class),
 *     @Filter(RateLimitFilter.class),
 *     @Filter(LoggingFilter.class)
 * })
 * public class SecureController {
 *     // All routes have all three filters applied
 * }
 * }</pre>
 *
 * <p>Same effect using repeated annotations (preferred):</p>
 * <pre>{@code
 * @RestController
 * @Filter(AuthFilter.class)
 * @Filter(RateLimitFilter.class)
 * @Filter(LoggingFilter.class)
 * public class SecureController {
 *     // All routes have all three filters applied
 * }
 * }</pre>
 *
 * @see Filter
 * @see net.magnesiumbackend.core.route.HttpFilter
 */
@Target({ ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Filters {
    /**
     * The filter annotations to apply.
     *
     * @return the array of filter annotations
     */
    Filter[] value();
}