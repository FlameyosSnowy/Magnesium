package net.magnesiumbackend.core.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Configuration source backed by environment variables.
 *
 * <p>Reads configuration from the system environment or a provided map.
 * Environment variables are typically used for deployment-specific
 * settings like database credentials, API keys, and feature flags.</p>
 *
 * <h3>Key Format</h3>
 * <p>Environment variable names are used as-is. For nested config,
 * use underscores in the variable name (e.g., {@code DB_HOST} maps
 * to key {@code DB_HOST}).</p>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * // Use system environment
 * ConfigSource env = new EnvConfigSource();
 *
 * // Or with custom map for testing
 * Map<String, String> testEnv = Map.of(
 *     "SERVER_PORT", "8080",
 *     "DB_HOST", "localhost"
 * );
 * ConfigSource testSource = new EnvConfigSource(testEnv);
 *
 * int port = env.getInt("SERVER_PORT");
 * }</pre>
 *
 * @see ConfigSource
 * @see MagnesiumConfigurationManager.Builder#env()
 */
public final class EnvConfigSource implements ConfigSource {

    private final Map<String, String> env;

    /**
     * Creates a source using the system environment variables.
     */
    public EnvConfigSource() {
        this(System.getenv());
    }

    /**
     * Creates a source using the provided environment map.
     *
     * @param env the environment variable map
     */
    public EnvConfigSource(@NotNull Map<String, String> env) {
        this.env = env;
    }

    @Override
    public @NotNull String name() {
        return "env";
    }

    @Override
    public boolean has(@NotNull String key) {
        return env.containsKey(key);
    }

    @Override
    public @Nullable String getString(@NotNull String key) {
        return env.get(key);
    }

    @Override
    public @Nullable Integer getInt(@NotNull String key) {
        String v = getString(key);
        if (v == null) return null;
        return Integer.parseInt(v.trim());
    }

    @Override
    public @Nullable Long getLong(@NotNull String key) {
        String v = getString(key);
        if (v == null) return null;
        return Long.parseLong(v.trim());
    }

    @Override
    public @Nullable Boolean getBoolean(@NotNull String key) {
        String v = getString(key);
        if (v == null) return null;
        return Boolean.parseBoolean(v.trim());
    }

    @Override
    public @Nullable Double getDouble(@NotNull String key) {
        String v = getString(key);
        if (v == null) return null;
        return Double.parseDouble(v.trim());
    }

    @Override
    public @Nullable Float getFloat(@NotNull String key) {
        String v = getString(key);
        if (v == null) return null;
        return Float.parseFloat(v.trim());
    }
}
