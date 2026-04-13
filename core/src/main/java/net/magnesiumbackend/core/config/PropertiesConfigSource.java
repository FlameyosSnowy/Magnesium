package net.magnesiumbackend.core.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Configuration source backed by Java Properties files.
 *
 * <p>Reads configuration from standard {@code .properties} files with
 * the familiar {@code key=value} format. Supports flat keys only;
 * for nested structures consider YAML or JSON.</p>
 *
 * <h3>Example Properties File</h3>
 * <pre>
 * server.port=8080
 * server.host=localhost
 * database.url=jdbc:postgresql://localhost/mydb
 * </pre>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * // Load from file
 * ConfigSource props = PropertiesConfigSource.fromPath(Path.of("application.properties"));
 *
 * // Or from existing Properties object
 * Properties p = new Properties();
 * p.load(inputStream);
 * ConfigSource source = new PropertiesConfigSource("custom", p);
 *
 * int port = props.getInt("server.port");
 * }</pre>
 *
 * @see ConfigSource
 * @see MagnesiumConfigurationManager.Builder#properties(Path)
 */
public final class PropertiesConfigSource implements ConfigSource {

    private final String name;
    private final Properties properties;

    /**
     * Creates a source from an existing Properties object.
     *
     * @param name       the source name
     * @param properties the properties to use
     */
    public PropertiesConfigSource(@NotNull String name, @NotNull Properties properties) {
        this.name = name;
        this.properties = properties;
    }

    /**
     * Loads properties from a file path.
     *
     * @param path the path to the properties file
     * @return a new PropertiesConfigSource
     * @throws IllegalStateException if the file cannot be read
     */
    public static @NotNull PropertiesConfigSource fromPath(@NotNull Path path) {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load properties from: " + path, e);
        }
        return new PropertiesConfigSource("properties:" + path, props);
    }

    @Override
    public @NotNull String name() {
        return name;
    }

    @Override
    public boolean has(@NotNull String key) {
        return properties.containsKey(key);
    }

    @Override
    public @Nullable String getString(@NotNull String key) {
        return properties.getProperty(key);
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
