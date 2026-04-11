package net.magnesiumbackend.core.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class EnvConfigSource implements ConfigSource {

    private final Map<String, String> env;

    public EnvConfigSource() {
        this(System.getenv());
    }

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
