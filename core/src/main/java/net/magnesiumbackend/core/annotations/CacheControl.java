package net.magnesiumbackend.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies HTTP cache control directives for a controller method's response.
 *
 * <p>This annotation is evaluated at compile time to generate optimized
 * Cache-Control header values and response metadata.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * {@code
 * @GetMapping(path = "/static/config")
 * @CacheControl(maxAge = 3600, isPublic = true)
 * public Config getConfig() {
 *     return configService.getConfig();
 * }
 * }
 * </pre>
 *
 * <p>The above generates:</p>
 * <pre>
 * Cache-Control: public, max-age=3600
 * </pre>
 *
 * @see Produces
 * @see GetMapping
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheControl {

    /**
     * The maximum time in seconds the response can be cached.
     *
     * <p>This is used for the {@code max-age} directive.</p>
     *
     * <p>Default: 0 (no caching)</p>
     *
     * @return max age in seconds
     */
    int maxAge() default 0;

    /**
     * Whether the response can be cached by any cache (public).
     *
     * <p>If {@code false}, only private caches (e.g., browser) can store the response.
     * If {@code true}, shared caches (e.g., CDN, proxy) may also cache it.</p>
     *
     * <p>Default: false</p>
     *
     * @return true if publicly cacheable
     */
    boolean isPublic() default false;

    /**
     * Forces revalidation on every request.
     *
     * <p>When {@code true}, caches must revalidate with the origin server before
     * using a cached copy. This generates the {@code no-cache} directive.</p>
     *
     * <p>Default: false</p>
     *
     * @return true if revalidation is required
     */
    boolean noCache() default false;

    /**
     * Prevents storing any version of the response.
     *
     * <p>When {@code true}, the response must not be stored in any cache.
     * This generates the {@code no-store} directive and takes precedence
     * over other settings.</p>
     *
     * <p>Default: false</p>
     *
     * @return true if storage is prohibited
     */
    boolean noStore() default false;
}
