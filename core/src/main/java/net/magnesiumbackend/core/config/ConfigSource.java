package net.magnesiumbackend.core.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

public interface ConfigSource {

    @NotNull String name();

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
}
