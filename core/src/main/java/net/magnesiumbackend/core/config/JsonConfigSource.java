package net.magnesiumbackend.core.config;

import com.dslplatform.json.DslJson;
import com.dslplatform.json.runtime.Settings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class JsonConfigSource implements ConfigSource {

    private static final DslJson<Object> DSL_JSON = new DslJson<>(Settings.withRuntime().includeServiceLoader());

    private final ConfigSource delegate;

    private JsonConfigSource(@NotNull ConfigSource delegate) {
        this.delegate = delegate;
    }

    public static @NotNull JsonConfigSource fromPath(@NotNull Path path) {
        Object loaded;
        try (InputStream in = Files.newInputStream(path)) {
            loaded = DSL_JSON.deserialize(Object.class, in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load json from: " + path, e);
        }

        if (!(loaded instanceof Map<?, ?> map)) {
            throw new IllegalStateException("JSON root must be an object: " + path);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> root = (Map<String, Object>) map;
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
