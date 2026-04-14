package net.magnesiumbackend.devtools;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

public class ClasspathWatcher {
    private final Path watchPath;
    private final long debounceMs;
    private final Consumer<Set<Path>> onChange;
    private final ScheduledExecutorService debouncer = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> pending;
    private final Set<Path> accumulated = ConcurrentHashMap.newKeySet();

    public ClasspathWatcher(Path watchPath, long debounceMs, Consumer<Set<Path>> onChange) {
        this.watchPath = watchPath;
        this.debounceMs = debounceMs;
        this.onChange = onChange;
    }

    public void start() throws IOException {
        WatchService watcher = FileSystems.getDefault().newWatchService();
        registerAll(watchPath, watcher);

        Thread.ofVirtual().start(() -> {
            while (true) {
                WatchKey key;
                try { key = watcher.take(); } catch (InterruptedException e) { break; }

                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == OVERFLOW) continue;
                    Path changed = watchPath.resolve((Path) event.context());
                    if (changed.toString().endsWith(".class")) {
                        accumulated.add(changed);
                        scheduleFlush();
                    }
                }
                key.reset();
            }
        });
    }

    private void scheduleFlush() {
        if (pending != null) pending.cancel(false);
        pending = debouncer.schedule(() -> {
            Set<Path> snapshot = new HashSet<>(accumulated);
            accumulated.clear();
            onChange.accept(snapshot);
        }, debounceMs, TimeUnit.MILLISECONDS);
    }

    private void registerAll(Path root, WatchService watcher) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public @NotNull FileVisitResult preVisitDirectory(@NotNull Path dir, @NotNull BasicFileAttributes attrs) throws IOException {
                dir.register(watcher, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}