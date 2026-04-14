package net.magnesiumbackend.devtools;

import net.magnesiumbackend.core.reload.ReloadListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public class HotReloadEngine {
    private static final Logger logger = LoggerFactory.getLogger(HotReloadEngine.class);

    private final ClasspathWatcher watcher;
    private final IncrementalCompiler compiler;
    private final Path classesDir;
    private final Set<String> frameworkPrefixes;
    private final ReloadListener listener;

    private ReloadClassLoader currentLoader;

    public HotReloadEngine(
        Path sourceRoot,
        Path classesDir,
        List<Path> classpath,
        Set<String> frameworkPrefixes,
        ReloadListener listener
    ) {
        this.classesDir = classesDir;
        this.frameworkPrefixes = frameworkPrefixes;
        this.listener = listener;
        this.compiler = new IncrementalCompiler(sourceRoot, classesDir, classpath);
        this.watcher = new ClasspathWatcher(classesDir, 200, this::onChanged);
    }

    public void start() throws IOException {
        currentLoader = newLoader();
        watcher.start();
    }

    private void onChanged(Set<Path> changed) {
        IncrementalCompiler.CompileResult result = compiler.compile(changed);
        if (!result.success()) {
            result.errors().forEach(logger::error);
            return;
        }

        ReloadClassLoader oldLoader = currentLoader;
        try {
            currentLoader = newLoader();
            listener.onReload(currentLoader);
        } catch (Exception e) {
            logger.error("[Magnesium] Reload failed: {}", e.getMessage());
            currentLoader = oldLoader; // roll back
            return;
        }

        oldLoader.dispose(); // eligible for GC
        logger.info("[Magnesium] Reloaded {} class(es)", changed.size());
    }

    private ReloadClassLoader newLoader() throws MalformedURLException {
        return new ReloadClassLoader(
            classesDir,
            Thread.currentThread().getContextClassLoader(),
            frameworkPrefixes
        );
    }
}