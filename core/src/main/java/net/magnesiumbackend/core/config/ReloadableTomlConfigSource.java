package net.magnesiumbackend.core.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A TOML configuration source that automatically reloads when the file changes.
 *
 * <p>This config source watches the TOML file for modifications and automatically
 * reloads the configuration without requiring an application restart. Value changes
 * are picked up immediately; adding/removing properties is supported as long as
 * the property was already defined in the code.</p>
 *
 * <p>This is the default and recommended way to load {@code application.toml} in
 * Magnesium applications during development.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * MagnesiumApplication.builder()
 *     .configuration(cfg -> cfg
 *         .toml(Path.of("application.toml"))  // This uses ReloadableTomlConfigSource by default
 *     )
 *     .build()
 *     .run(8080);
 * }</pre>
 *
 * @see TomlConfigSource
 * @see MagnesiumConfigurationManager.Builder#toml(Path)
 */
public class ReloadableTomlConfigSource implements ConfigSource, AutoCloseable, ReloadableSource {

    private final String name;
    private final Path path;
    private final AtomicReference<TomlConfigSource> delegate;
    private final WatchService watchService;
    private final Thread watchThread;
    private volatile boolean running = true;
    private final java.util.List<Runnable> reloadCallbacks = new java.util.ArrayList<>();

    private ReloadableTomlConfigSource(@NotNull String name, @NotNull Path path) {
        this.name = name;
        this.path = path.toAbsolutePath().normalize();
        this.delegate = new AtomicReference<>(loadSource());

        try {
            this.watchService = FileSystems.getDefault().newWatchService();
            this.path.getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

            this.watchThread = Thread.ofVirtual().name("toml-reloader-" + path.getFileName()).start(this::watchLoop);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize file watcher for: " + path, e);
        }
    }

    /**
     * Creates a reloadable TOML config source from a file path.
     *
     * @param path the path to the TOML file
     * @return a new reloadable TOML config source
     */
    public static @NotNull ReloadableTomlConfigSource fromPath(@NotNull Path path) {
        return new ReloadableTomlConfigSource("toml:" + path, path);
    }

    private TomlConfigSource loadSource() {
        return TomlConfigSource.fromPath(path);
    }

    private void watchLoop() {
        while (running) {
            try {
                WatchKey key = watchService.poll(1000, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (key == null) continue;

                for (WatchEvent<?> event : key.pollEvents()) {
                    Path changed = (Path) event.context();
                    if (path.getFileName().equals(changed)) {
                        // Debounce: wait a bit for write to complete
                        Thread.sleep(100);
                        try {
                            TomlConfigSource newSource = loadSource();
                            delegate.set(newSource);
                            System.out.println("[Magnesium] Reloaded configuration from: " + path);
                            // Notify listeners
                            reloadCallbacks.forEach(Runnable::run);
                        } catch (Exception e) {
                            System.err.println("[Magnesium] Failed to reload configuration: " + e.getMessage());
                        }
                    }
                }
                key.reset();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @Override
    public @NotNull String name() {
        return name;
    }

    @Override
    public boolean has(@NotNull String key) {
        return delegate.get().has(key);
    }

    @Override
    public @Nullable String getString(@NotNull String key) {
        return delegate.get().getString(key);
    }

    @Override
    public @Nullable Integer getInt(@NotNull String key) {
        return delegate.get().getInt(key);
    }

    @Override
    public @Nullable Long getLong(@NotNull String key) {
        return delegate.get().getLong(key);
    }

    @Override
    public @Nullable Boolean getBoolean(@NotNull String key) {
        return delegate.get().getBoolean(key);
    }

    @Override
    public @Nullable Double getDouble(@NotNull String key) {
        return delegate.get().getDouble(key);
    }

    @Override
    public @Nullable Float getFloat(@NotNull String key) {
        return delegate.get().getFloat(key);
    }

    @Override
    public <E extends Enum<E>> @Nullable E getEnum(@NotNull String key, @NotNull Class<E> enumType) {
        return delegate.get().getEnum(key, enumType);
    }

    @Override
    public void close() throws Exception {
        running = false;
        watchThread.interrupt();
        watchThread.join(5000);
        watchService.close();
    }

    @Override
    public void onReload(Runnable callback) {
        reloadCallbacks.add(callback);
    }
}
