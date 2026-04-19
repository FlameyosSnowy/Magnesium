package net.magnesiumbackend.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a type-safe application properties container.
 *
 * <p>Classes annotated with {@code @ApplicationProperties} are processed at compile time
 * to generate properties loaders. These provide type-safe access to configuration values
 * from properties files, YAML, JSON, or environment variables.</p>
 *
 * <p>Unlike {@link ApplicationConfiguration} which works with records and immutable types,
 * this annotation is designed for mutable property classes with getters/setters or public fields,
 * similar to Spring Boot's {@code @ConfigurationProperties} but with compile-time validation.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @ApplicationProperties(prefix = "app.database")
 * public class DatabaseProperties {
 *     @ApplicationProperty(name = "host", defaultValue = "localhost")
 *     private String host;
 *
 *     @ApplicationProperty(name = "port", defaultValue = "5432")
 *     private int port;
 *
 *     @ApplicationProperty(name = "url", required = true)
 *     private String url;
 *
 *     // Getters and setters (or use Lombok @Data)
 *     public String getHost() { return host; }
 *     public void setHost(String host) { this.host = host; }
 *     // ... etc
 * }
 * }</pre>
 *
 * <p>Configuration file (application.yml):</p>
 * <pre>
 * app:
 *   database:
 *     host: db.example.com
 *     port: 5432
 *     url: jdbc:postgresql://db.example.com:5432/mydb
 * </pre>
 *
 * <p>Retrieving properties at runtime:</p>
 * <pre>{@code
 * // In Application.configure():
 * runtime.properties(DatabaseProperties.class);
 *
 * // Later, in a service:
 * DatabaseProperties dbProps = serviceContext.configurationManager()
 *     .get(DatabaseProperties.class);
 * }</pre>
 *
 * @see ApplicationProperty
 * @see ApplicationConfiguration
 * @see net.magnesiumbackend.core.config.MagnesiumConfigurationManager
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface ApplicationProperties {
    /**
     * The configuration prefix for all properties in this class.
     *
     * <p>For example, if prefix is {@code "app.database"}, a property
     * defined as {@code @ApplicationProperty(name = "host")} will look up
     * the key {@code "app.database.host"} in configuration sources.</p>
     *
     * @return the configuration key prefix
     */
    String prefix();

    /**
     * Whether to ignore unknown properties during binding.
     *
     * <p>If false (default), unknown properties will cause an error.
     * If true, unknown properties are silently ignored.</p>
     *
     * @return true to ignore unknown properties
     */
    boolean ignoreUnknownProperties() default false;

    /**
     * Whether this properties class should be validated at startup.
     *
     * <p>If true (default), all {@code required = true} properties must
     * be present or the application fails to start.</p>
     *
     * @return true to validate on startup
     */
    boolean validateOnStartup() default true;
}
