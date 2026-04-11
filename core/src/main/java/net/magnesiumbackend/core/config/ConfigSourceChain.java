package net.magnesiumbackend.core.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ConfigSourceChain implements ConfigSource {

    private final String name;
    private final List<ConfigSource> sources;

    public ConfigSourceChain(@NotNull String name, @NotNull List<ConfigSource> sources) {
        this.name = name;
        this.sources = sources;
    }

    @Override
    public @NotNull String name() {
        return name;
    }

    @Override
    public boolean has(@NotNull String key) {
        for (ConfigSource source : sources) {
            if (source.has(key)) return true;
        }
        return false;
    }

    @Override
    public @Nullable String getString(@NotNull String key) {
        for (ConfigSource source : sources) {
            String v = source.getString(key);
            if (v != null) return v;
        }
        return null;
    }

    @Override
    public @Nullable Integer getInt(@NotNull String key) {
        for (ConfigSource source : sources) {
            Integer v = source.getInt(key);
            if (v != null) return v;
        }
        return null;
    }

    @Override
    public @Nullable Long getLong(@NotNull String key) {
        for (ConfigSource source : sources) {
            Long v = source.getLong(key);
            if (v != null) return v;
        }
        return null;
    }

    @Override
    public @Nullable Boolean getBoolean(@NotNull String key) {
        for (ConfigSource source : sources) {
            Boolean v = source.getBoolean(key);
            if (v != null) return v;
        }
        return null;
    }

    @Override
    public @Nullable Double getDouble(@NotNull String key) {
        for (ConfigSource source : sources) {
            Double v = source.getDouble(key);
            if (v != null) return v;
        }
        return null;
    }

    @Override
    public @Nullable Float getFloat(@NotNull String key) {
        for (ConfigSource source : sources) {
            Float v = source.getFloat(key);
            if (v != null) return v;
        }
        return null;
    }
}
