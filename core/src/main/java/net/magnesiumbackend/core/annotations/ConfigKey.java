package net.magnesiumbackend.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds a record component to a configuration key path.
 *
 * <p>Used within {@link ApplicationConfiguration} annotated records to specify
 * the configuration key that provides the value for a field. Supports nested
 * configuration via dot notation.</p>
 *
 * <p>The key lookup follows the chain of configured sources in order
 * (properties, YAML, JSON, environment variables) and returns the first match.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @ApplicationConfiguration
 * public record ServerConfig(
 *     @ConfigKey("server.host") String host,
 *     @ConfigKey("server.port") int port,
 *     @ConfigKey("server.ssl.enabled") boolean sslEnabled
 * ) {}
 * }</pre>
 *
 * <p>Configuration file (YAML):</p>
 * <pre>
 * server:
 *   host: localhost
 *   port: 8080
 *   ssl:
 *     enabled: true
 * </pre>
 *
 * @see ApplicationConfiguration
 * @see net.magnesiumbackend.core.config.MagnesiumConfigurationManager
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface ConfigKey {
    /**
     * The configuration key path.
     *
     * <p>Use dot notation for nested keys, e.g., {@code "database.pool.max-size"}.
     * The key is case-sensitive and must match the configuration source exactly.</p>
     *
     * @return the configuration key path
     */
    String value();
}
