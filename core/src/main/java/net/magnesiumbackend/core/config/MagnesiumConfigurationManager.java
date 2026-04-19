package net.magnesiumbackend.core.config;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

public final class MagnesiumConfigurationManager {

    private final ConfigSource source;
    private final Map<Class<?>, Object> cache = new HashMap<>();
    private final Map<Class<?>, GeneratedConfigClass> loaders = new HashMap<>();

    private MagnesiumConfigurationManager(@NotNull ConfigSource source) {
        this.source = source;
        for (GeneratedConfigClass loader : ServiceLoader.load(GeneratedConfigClass.class)) {
            loaders.put(loader.configType(), loader);
        }
    }

    public @NotNull ConfigSource source() {
        return source;
    }

    @SuppressWarnings("unchecked")
    public synchronized <T> @NotNull T get(@NotNull Class<T> type) {
        Object cached = cache.get(type);
        if (cached != null) return (T) cached;

        GeneratedConfigClass loader = loaders.get(type);
        if (loader == null) {
            throw new IllegalStateException(
                "No GeneratedConfigClass found for type: " + type.getName() + ". "
                    + "Either annotate it with @ApplicationConfiguration (codegen), or provide a service-loaded GeneratedConfigClass implementation.");
        }

        Object loaded = loader.load(source);
        if (loaded == null) {
            throw new IllegalStateException("Config loader for " + type.getName() + " returned null.");
        }

        cache.put(type, loaded);
        return (T) loaded;
    }

    /**
     * Clears the configuration cache, forcing all configurations to be reloaded
     * from the source on next access.
     *
     * <p>This is useful when using {@link ReloadableTomlConfigSource} - call this
     * method after the configuration file has been modified to pick up changes.</p>
     *
     * <p>Note: This method is automatically called by ReloadableTomlConfigSource
     * when the file changes, so manual invocation is usually not necessary.</p>
     */
    public synchronized void refresh() {
        cache.clear();
    }

    public static @NotNull Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<ConfigSource> sources = new ArrayList<>();
        private final List<ReloadableSource> reloadableSources = new ArrayList<>();

        private Builder() {}

        public @NotNull Builder source(@NotNull ConfigSource source) {
            this.sources.add(source);
            if (source instanceof ReloadableSource reloadable) {
                this.reloadableSources.add(reloadable);
            }
            return this;
        }

        public @NotNull Builder properties(@NotNull Path path) {
            return source(PropertiesConfigSource.fromPath(path));
        }

        public @NotNull Builder yaml(@NotNull Path path) {
            return source(YamlConfigSource.fromPath(path));
        }

        public @NotNull Builder json(@NotNull Path path) {
            return source(JsonConfigSource.fromPath(path));
        }

        public @NotNull Builder env() {
            return source(new EnvConfigSource());
        }

        public @NotNull Builder toml(@NotNull Path path) {
            ReloadableTomlConfigSource source = ReloadableTomlConfigSource.fromPath(path);
            sources.add(source);
            reloadableSources.add(source);
            return this;
        }

        public @NotNull MagnesiumConfigurationManager build() {
            ConfigSource chain = sources.isEmpty()
                ? new ConfigSourceChain("empty", List.of())
                : new ConfigSourceChain("chain", List.copyOf(sources));
            MagnesiumConfigurationManager manager = new MagnesiumConfigurationManager(chain);

            // Register reload callbacks to clear cache when config changes
            Runnable refreshCallback = manager::refresh;
            for (ReloadableSource reloadable : reloadableSources) {
                reloadable.onReload(refreshCallback);
            }

            return manager;
        }
    }
}
