package net.magnesiumbackend.core.extensions;

import net.magnesiumbackend.core.MagnesiumRuntime;
import org.jetbrains.annotations.NotNull;

public interface MagnesiumExtension {
    void configure(@NotNull MagnesiumRuntime runtime);

    @NotNull String name();
}
