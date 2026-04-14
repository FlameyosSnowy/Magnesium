package net.magnesiumbackend.core.config;

import net.magnesiumbackend.core.json.JsonProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Configuration source backed by JSON files.
 *
 * <p>Reads configuration from JSON files using the configured {@link JsonProvider}.
 * Supports nested objects via dot notation (e.g., {@code "server.port"} accesses
 * {@code {"server": {"port": 8080}}}).</p>
 *
 * <h3>Example JSON</h3>
 * <pre>
 * {
 *   "server": {
 *     "port": 8080,
 *     "host": "localhost"
 *   },
 *   "database": {
 *     "url": "jdbc:postgresql://localhost/mydb"
 *   }
 * }
 * </pre>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * ConfigSource json = JsonConfigSource.fromPath(Path.of("config.json"));
 *
 * int port = json.getInt("server.port");
 * String host = json.getString("server.host");
 * }</pre>
 *
 * @see ConfigSource
 * @see MagnesiumConfigurationManager.Builder#json(Path)
 */
public final class JsonConfigSource implements ConfigSource {

    private static JsonProvider jsonProvider;

    private final ConfigSource delegate;

    public static void init(JsonProvider provider) {
        jsonProvider = provider;
    }

    private JsonConfigSource(@NotNull ConfigSource delegate) {
        this.delegate = delegate;
    }

    /**
     * Loads JSON configuration from a file path.
     *
     * @param path the path to the JSON file
     * @return a new JsonConfigSource
     * @throws IllegalStateException if the file cannot be read or parsed
     */
    public static @NotNull JsonConfigSource fromPath(@NotNull Path path) {
        Map<String, Object> root;
        try (InputStream in = Files.newInputStream(path)) {
            root = jsonProvider.deserializeToMap(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load json from: " + path, e);
        }
        return new JsonConfigSource(new MapConfigSource("json:" + path, root));
    }

    @Override
    public @NotNull String name() {
        return delegate.name();
    }

    @Override
    public boolean has(@NotNull String key) {
        return delegate.has(key);
    }

    @Override
    public @Nullable String getString(@NotNull String key) {
        return delegate.getString(key);
    }

    @Override
    public @Nullable Integer getInt(@NotNull String key) {
        return delegate.getInt(key);
    }

    @Override
    public @Nullable Long getLong(@NotNull String key) {
        return delegate.getLong(key);
    }

    @Override
    public @Nullable Boolean getBoolean(@NotNull String key) {
        return delegate.getBoolean(key);
    }

    @Override
    public @Nullable Double getDouble(@NotNull String key) {
        return delegate.getDouble(key);
    }

    @Override
    public @Nullable Float getFloat(@NotNull String key) {
        return delegate.getFloat(key);
    }
}
