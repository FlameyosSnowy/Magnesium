package net.magnesiumbackend.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds a method parameter to a path variable extracted from the request URL.
 *
 * <p>Path variables are defined in the route pattern using curly braces,
 * e.g., {@code "/users/{id}"}. The value inside the braces is the name of
 * the path variable.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @GetMapping(path = "/users/{id}")
 * public ResponseEntity<User> getUser(@PathParam String id) {
 *     return ResponseEntity.ok(userService.findById(id));
 * }
 * }</pre>
 *
 * <p>Type conversion is automatically applied for common types:
 * <ul>
 *   <li>{@code String} - used as-is</li>
 *   <li>{@code int}, {@code Integer} - parsed with {@code Integer.parseInt()}</li>
 *   <li>{@code long}, {@code Long} - parsed with {@code Long.parseLong()}</li>
 *   <li>{@code UUID} - parsed with {@code UUID.fromString()}</li>
 *   <li>Types with {@code static parse(String)} method</li>
 *   <li>Types with {@code String} constructor</li>
 * </ul>
 *
 * @see GetMapping
 * @see PostMapping
 * @see PutMapping
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.PARAMETER)
public @interface PathParam {
    /**
     * The name of the path variable.
     *
     * <p>If not specified, the parameter name is used.</p>
     *
     * @return the path variable name
     */
    String value() default "";
}
