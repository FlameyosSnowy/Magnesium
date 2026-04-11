package net.magnesiumbackend.core.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

final class MapConfigSource implements ConfigSource {

    private final String name;
    private final Map<String, Object> root;

    MapConfigSource(@NotNull String name, @NotNull Map<String, Object> root) {
        this.name = name;
        this.root = root;
    }

    @Override
    public @NotNull String name() {
        return name;
    }

    @Override
    public boolean has(@NotNull String key) {
        return resolve(key) != null;
    }

    @Override
    public @Nullable String getString(@NotNull String key) {
        Object v = resolve(key);
        if (v == null) return null;
        if (v instanceof String s) return s;
        return String.valueOf(v);
    }

    @Override
    public @Nullable Integer getInt(@NotNull String key) {
        Object v = resolve(key);
        return switch (v) {
            case Number n -> n.intValue();
            case String s -> Integer.parseInt(s.trim());
            case null, default -> null;
        };
    }

    @Override
    public @Nullable Long getLong(@NotNull String key) {
        Object v = resolve(key);
        return switch (v) {
            case Number n -> n.longValue();
            case String s -> Long.parseLong(s.trim());
            case null, default -> null;
        };
    }

    @Override
    public @Nullable Boolean getBoolean(@NotNull String key) {
        Object v = resolve(key);
        return switch (v) {
            case null -> null;
            case Boolean b -> b;
            case String s -> Boolean.parseBoolean(s.trim());
            default -> null;
        };
    }

    @Override
    public @Nullable Double getDouble(@NotNull String key) {
        Object v = resolve(key);
        return switch (v) {
            case Number n -> n.doubleValue();
            case String s -> Double.parseDouble(s.trim());
            case null, default -> null;
        };
    }

    @Override
    public @Nullable Float getFloat(@NotNull String key) {
        Object v = resolve(key);
        return switch (v) {
            case Number n -> n.floatValue();
            case String s -> Float.parseFloat(s.trim());
            case null, default -> null;
        };
    }

    private @Nullable Object resolve(@NotNull String key) {
        if (key.isEmpty()) return null;

        Object current = root;
        String[] parts = key.split("\\.");

        for (String part : parts) {
            if (!(current instanceof Map<?, ?> m)) return null;
            current = m.get(part);
            if (current == null) return null;
        }

        if (current instanceof List<?> l && l.size() == 1) {
            return l.getFirst();
        }

        return current;
    }
}
