package net.magnesiumbackend.core.config;

import org.jetbrains.annotations.NotNull;

public interface GeneratedConfigClass {

    @NotNull Class<?> configType();

    @NotNull Object load(@NotNull ConfigSource source);
}
