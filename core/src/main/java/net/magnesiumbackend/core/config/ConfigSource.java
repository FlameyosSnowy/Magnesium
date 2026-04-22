package net.magnesiumbackend.core.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Abstraction for reading configuration values from various sources.
 *
 * <p>ConfigSource implementations provide a unified interface for accessing
 * configuration from properties files, YAML, JSON, environment variables,
 * and other sources. Sources support nested keys using dot notation (e.g.,
 * {@code "server.port"}).</p>
 *
 * <h3>Supported Value Types</h3>
 * <ul>
 *   <li>String - Raw configuration values</li>
 *   <li>Integer - Whole numbers</li>
 *   <li>Long - Large whole numbers</li>
 *   <li>Boolean - true/false values</li>
 *   <li>Double - Decimal numbers</li>
 *   <li>Float - Single-precision decimals</li>
 *   <li>Enum - Enumerated types</li>
 * </ul>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * ConfigSource source = YamlConfigSource.fromPath(Path.of("config.yml"));
 *
 * // Safe access with defaults
 * int port = source.getInt("server.port");
 * String host = source.getString("server.host");
 *
 * // Require presence (throws if missing)
 * String dbUrl = source.requireString("database.url");
 *
 * // Optional values
 * Optional<String> optional = source.getOptionalString("feature.flag");
 * }</pre>
 *
 * @see MagnesiumConfigurationManager
 * @see ConfigSourceChain
 * @see YamlConfigSource
 * @see PropertiesConfigSource
 */
public interface ConfigSource {

    /**
     * Returns the name of this configuration source.
     *
     * @return a descriptive name (e.g., "yaml:config.yml", "env")
     */
    @NotNull String name();

    /**
     * Checks if the specified key exists in this source.
     *
     * @param key the configuration key (supports dot notation)
     * @return true if the key exists
     */
    boolean has(@NotNull String key);

    @Nullable String getString(@NotNull String key);

    default @NotNull String requireString(@NotNull String key) {
        String value = getString(key);
        if (value == null) {
            throw new IllegalStateException("Missing required config value: '" + key + "' from source: " + name());
        }
        return value;
    }

    default Optional<String> getOptionalString(@NotNull String key) {
        return Optional.ofNullable(getString(key));
    }

    @Nullable Integer getInt(@NotNull String key);

    default int requireInt(@NotNull String key) {
        Integer value = getInt(key);
        if (value == null) {
            throw new IllegalStateException("Missing required config value: '" + key + "' from source: " + name());
        }
        return value;
    }

    default OptionalInt getOptionalInt(@NotNull String key) {
        Integer value = getInt(key);
        return value == null ? OptionalInt.empty() : OptionalInt.of(value);
    }

    @Nullable Long getLong(@NotNull String key);

    default long requireLong(@NotNull String key) {
        Long value = getLong(key);
        if (value == null) {
            throw new IllegalStateException("Missing required config value: '" + key + "' from source: " + name());
        }
        return value;
    }

    default OptionalLong getOptionalLong(@NotNull String key) {
        Long value = getLong(key);
        return value == null ? OptionalLong.empty() : OptionalLong.of(value);
    }

    @Nullable Boolean getBoolean(@NotNull String key);

    default boolean requireBoolean(@NotNull String key) {
        Boolean value = getBoolean(key);
        if (value == null) {
            throw new IllegalStateException("Missing required config value: '" + key + "' from source: " + name());
        }
        return value;
    }

    @Nullable Double getDouble(@NotNull String key);

    default double requireDouble(@NotNull String key) {
        Double value = getDouble(key);
        if (value == null) {
            throw new IllegalStateException("Missing required config value: '" + key + "' from source: " + name());
        }
        return value;
    }

    default OptionalDouble getOptionalDouble(@NotNull String key) {
        Double value = getDouble(key);
        return value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
    }

    @Nullable Float getFloat(@NotNull String key);

    default float requireFloat(@NotNull String key) {
        Float value = getFloat(key);
        if (value == null) {
            throw new IllegalStateException("Missing required config value: '" + key + "' from source: " + name());
        }
        return value;
    }

    default Optional<Float> getOptionalFloat(@NotNull String key) {
        return Optional.ofNullable(getFloat(key));
    }

    default <E extends Enum<E>> @Nullable E getEnum(@NotNull String key, @NotNull Class<E> enumType) {
        String raw = getString(key);
        if (raw == null) return null;
        return Enum.valueOf(enumType, raw);
    }

    static ConfigSource ofMap(String name, Map<String, Object> map) {
        return new MapConfigSource(name, map);
    }

    static ConfigSource ofMap(Map<String, Object> map) {
        return new MapConfigSource("map", map);
    }

    static ConfigSource empty() {
        return new MapConfigSource("empty", new HashMap<>());
    }
}
