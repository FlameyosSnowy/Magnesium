package net.magnesiumbackend.core.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class PropertiesConfigSource implements ConfigSource {

    private final String name;
    private final Properties properties;

    public PropertiesConfigSource(@NotNull String name, @NotNull Properties properties) {
        this.name = name;
        this.properties = properties;
    }

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
