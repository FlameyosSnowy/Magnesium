package net.magnesiumbackend.core.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.snakeyaml.engine.v2.api.Load;
import org.snakeyaml.engine.v2.api.LoadSettings;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class YamlConfigSource implements ConfigSource {

    private final ConfigSource delegate;

    private YamlConfigSource(@NotNull ConfigSource delegate) {
        this.delegate = delegate;
    }

    public static @NotNull YamlConfigSource fromPath(@NotNull Path path) {
        Object loaded;
        LoadSettings settings = LoadSettings.builder().build();
        Load load = new Load(settings);

        try (InputStream in = Files.newInputStream(path)) {
            loaded = load.loadFromInputStream(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load yaml from: " + path, e);
        }

        if (!(loaded instanceof Map<?, ?> map)) {
            throw new IllegalStateException("YAML root must be a mapping/object: " + path);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> root = (Map<String, Object>) map;
        return new YamlConfigSource(new MapConfigSource("yaml:" + path, root));
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
