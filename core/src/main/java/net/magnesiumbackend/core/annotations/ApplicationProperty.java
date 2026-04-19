package net.magnesiumbackend.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a single property within an {@link ApplicationProperties} class.
 *
 * <p>Marks a field or setter method as a configuration property with
 * metadata for binding, validation, and documentation.</p>
 *
 * <p>Supported property types:</p>
 * <ul>
 *   <li>String</li>
 *   <li>int / Integer</li>
 *   <li>long / Long</li>
 *   <li>boolean / Boolean</li>
 *   <li>double / Double</li>
 *   <li>float / Float</li>
 *   <li>Enum types</li>
 *   <li>Duration (ISO-8601 format, e.g., "PT30S", "PT5M", "P1D")</li>
 *   <li>DataSize (e.g., "10MB", "1GB")</li>
 *   <li>List/Set of above types</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @ApplicationProperties(prefix = "server")
 * public class ServerProperties {
 *
 *     @ApplicationProperty(
 *         name = "host",
 *         defaultValue = "0.0.0.0",
 *         description = "The host address to bind to"
 *     )
 *     private String host;
 *
 *     @ApplicationProperty(
 *         name = "port",
 *         defaultValue = "8080",
 *         description = "The port to listen on"
 *     )
 *     private int port;
 *
 *     @ApplicationProperty(
 *         name = "timeout",
 *         defaultValue = "30s",
 *         description = "Request timeout"
 *     )
 *     private Duration timeout;
 *
 *     @ApplicationProperty(
 *         name = "max-body-size",
 *         defaultValue = "10MB",
 *         description = "Maximum request body size"
 *     )
 *     private DataSize maxBodySize;
 *
 *     @ApplicationProperty(
 *         name = "features",
 *         description = "Enabled features"
 *     )
 *     private List<String> features;
 *
 *     @ApplicationProperty(
 *         name = "mode",
 *         required = true,
 *         description = "Server mode (DEVELOPMENT, STAGING, PRODUCTION)"
 *     )
 *     private ServerMode mode;  // enum
 *
 *     // Getters and setters...
 * }
 * }</pre>
 *
 * @see ApplicationProperties
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface ApplicationProperty {
    /**
     * The property name (without the prefix).
     *
     * <p>The full configuration key is constructed as:
     * {@code prefix + "." + name}. For example, with prefix "server"
     * and name "port", the key is "server.port".</p>
     *
     * @return the property name
     */
    String name();

    /**
     * The default value to use when the property is not configured.
     *
     * <p>Must be specified as a string and will be converted to the
     * target type at runtime. For complex types like Duration or DataSize,
     * use their string representation (e.g., "30s", "10MB").</p>
     *
     * <p>If not specified and {@code required = false}, primitives default
     * to 0/false, objects to null, and collections to empty.</p>
     *
     * @return the default value as a string
     */
    String defaultValue() default "";

    /**
     * Whether this property is required.
     *
     * <p>If true and no value is configured, the application fails
     * to start with a clear error message. Required properties cannot
     * have default values.</p>
     *
     * @return true if required
     */
    boolean required() default false;

    /**
     * A description of this property for documentation and error messages.
     *
     * @return human-readable description
     */
    String description() default "";

    /**
     * Validation pattern for String properties (regex).
     *
     * <p>If specified, the property value must match this pattern.
     * Only applies to String type properties.</p>
     *
     * @return regex pattern for validation
     */
    String pattern() default "";

    /**
     * Minimum value for numeric properties.
     *
     * <p>Only applies to numeric types (int, long, double, float).
     * The value must be >= min.</p>
     *
     * @return minimum allowed value
     */
    long min() default Long.MIN_VALUE;

    /**
     * Maximum value for numeric properties.
     *
     * <p>Only applies to numeric types (int, long, double, float).
     * The value must be <= max.</p>
     *
     * @return maximum allowed value
     */
    long max() default Long.MAX_VALUE;
}
