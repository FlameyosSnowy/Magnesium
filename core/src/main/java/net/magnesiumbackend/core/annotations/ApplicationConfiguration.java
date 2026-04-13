package net.magnesiumbackend.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as an application configuration type.
 *
 * <p>Classes annotated with {@code @ApplicationConfiguration} are processed at compile time
 * to generate configuration loaders. These loaders enable type-safe access to configuration
 * values from properties, YAML, JSON, or environment variables via the
 * {@link net.magnesiumbackend.core.config.MagnesiumConfigurationManager}.</p>
 *
 * <p>Configuration classes are typically immutable records with nested support for
 * hierarchical configuration structures.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @ApplicationConfiguration
 * public record DatabaseConfig(
 *     @ConfigKey("db.host") String host,
 *     @ConfigKey("db.port") int port,
 *     @ConfigKey("db.username") String username,
 *     @ConfigKey("db.password") String password,
 *     @ConfigKey("db.pool") ConnectionPoolConfig pool
 * ) {}
 *
 * @ApplicationConfiguration
 * public record ConnectionPoolConfig(
 *     @ConfigKey("db.pool.min-size") int minSize,
 *     @ConfigKey("db.pool.max-size") int maxSize
 * ) {}
 * }</pre>
 *
 * <p>Retrieving configuration at runtime:</p>
 * <pre>{@code
 * MagnesiumApplication.builder()
 *     .configuration(cfg -> cfg
 *         .yaml(Path.of("config.yml"))
 *         .env())
 *     .build()
 *     .run(8080);
 *
 * // Later, in a service:
 * DatabaseConfig dbConfig = serviceContext.configurationManager()
 *     .get(DatabaseConfig.class);
 * }</pre>
 *
 * @see ConfigKey
 * @see net.magnesiumbackend.core.config.MagnesiumConfigurationManager
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface ApplicationConfiguration {
}
