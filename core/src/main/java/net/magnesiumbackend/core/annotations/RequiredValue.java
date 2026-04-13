package net.magnesiumbackend.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field or parameter as required in configuration or request binding.
 *
 * <p>Used primarily with configuration classes annotated with
 * {@link ApplicationConfiguration} to indicate that a configuration value
 * must be present. If the value is missing, the application fails to start
 * with a clear error message.</p>
 *
 * <p>Can also be used in request/response binding contexts to mark fields
 * that must be present in the payload.</p>
 *
 * <p>Example usage in configuration:</p>
 * <pre>{@code
 * @ApplicationConfiguration
 * public record DatabaseConfig(
 *     @ConfigKey("db.host") @RequiredValue String host,
 *     @ConfigKey("db.port") int port,  // optional, defaults to 0 if missing
 *     @ConfigKey("db.password") @RequiredValue String password
 * ) {}
 * }</pre>
 *
 * <p>If {@code db.host} or {@code db.password} is not defined in any
 * configuration source, the application fails fast during startup.</p>
 *
 * @see ApplicationConfiguration
 * @see ConfigKey
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface RequiredValue {
}
