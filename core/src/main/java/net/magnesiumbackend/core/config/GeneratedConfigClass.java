package net.magnesiumbackend.core.config;

import org.jetbrains.annotations.NotNull;

/**
 * Implemented by every compile-time-generated configuration loader.
 *
 * <p>The annotation processor generates one implementation per class annotated with
 * {@link net.magnesiumbackend.core.annotations.ApplicationConfiguration}. Each
 * generated loader knows how to extract values from a {@link ConfigSource} and
 * instantiate the configuration type.</p>
 *
 * <p>Generated implementations handle:
 * <ul>
 *   <li>Reading values from the config source using {@code @ConfigKey} annotations</li>
 *   <li>Type conversion (String, int, boolean, nested objects, lists)</li>
 *   <li>Validation of {@code @RequiredValue} fields</li>
 *   <li>Instantiating the configuration record or class</li>
 * </ul>
 * </p>
 *
 * <p>These loaders are discovered via {@link java.util.ServiceLoader} and registered
 * with the {@link MagnesiumConfigurationManager} at startup.</p>
 *
 * @see net.magnesiumbackend.core.annotations.ApplicationConfiguration
 * @see net.magnesiumbackend.core.annotations.ConfigKey
 * @see ConfigSource
 * @see MagnesiumConfigurationManager
 */
public interface GeneratedConfigClass {

    /**
     * Returns the configuration type this loader creates.
     *
     * @return the configuration class or record type
     */
    @NotNull Class<?> configType();

    /**
     * Loads and instantiates the configuration type from the given source.
     *
     * <p>Reads all required values from the config source, performs type conversion,
     * validates required fields, and returns a fully populated configuration instance.</p>
     *
     * @param source the configuration source (properties, YAML, environment, etc.)
     * @return the instantiated configuration object
     * @throws IllegalStateException if required values are missing or type conversion fails
     */
    @NotNull Object load(@NotNull ConfigSource source);
}
