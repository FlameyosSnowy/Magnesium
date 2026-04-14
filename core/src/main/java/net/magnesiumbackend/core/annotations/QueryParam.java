package net.magnesiumbackend.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds a method parameter to a query parameter from the request URL.
 *
 * <p>Query parameters are key-value pairs in the URL after the {@code ?},
 * e.g., {@code "/users?page=1&size=20"}. This annotation extracts the value
 * of a specific query parameter and binds it to the annotated method parameter.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @GetMapping(path = "/users")
 * public ResponseEntity<List<User>> getUsers(
 *         @QueryParam("page") int page,
 *         @QueryParam("size") int size,
 *         @QueryParam(value = "sort", required = false) String sort) {
 *     return ResponseEntity.ok(userService.findAll(page, size, sort));
 * }
 * }</pre>
 *
 * <p>Type conversion is automatically applied for common types:
 * <ul>
 *   <li>{@code String} - used as-is</li>
 *   <li>{@code int}, {@code Integer} - parsed with {@code Integer.parseInt()}</li>
 *   <li>{@code long}, {@code Long} - parsed with {@code Long.parseLong()}</li>
 *   <li>{@code boolean}, {@code Boolean} - parsed with {@code Boolean.parseBoolean()}</li>
 *   <li>{@code double}, {@code Double} - parsed with {@code Double.parseDouble()}</li>
 *   <li>{@code UUID} - parsed with {@code UUID.fromString()}</li>
 *   <li>{@code List<T>} - multiple values with same key, parsed individually</li>
 *   <li>Types with {@code static parse(String)} or {@code valueOf(String)} method</li>
 *   <li>Types with {@code String} constructor</li>
 * </ul>
 *
 * @see GetMapping
 * @see PostMapping
 * @see PutMapping
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.PARAMETER)
public @interface QueryParam {
    /**
     * The name of the query parameter.
     *
     * @return the query parameter name
     */
    String value();

    /**
     * Whether the query parameter is required.
     *
     * <p>If {@code true} (default) and the parameter is not present in the request,
     * an error will be raised. If {@code false}, the parameter will be {@code null}
     * (or default value for primitives) when not present.</p>
     *
     * @return whether the parameter is required
     */
    boolean required() default true;

    /**
     * The default value to use when the parameter is not present.
     *
     * <p>Only used when {@code required} is {@code false}. An empty string
     * indicates no default value.</p>
     *
     * @return the default value expression
     */
    String defaultValue() default "";
}
