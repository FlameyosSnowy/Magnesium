package net.magnesiumbackend.devtools;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Set;

public class ReloadClassLoader extends URLClassLoader {
    private final Set<String> parentPrefixes;

    // parentPrefixes: packages that must come from the parent (framework internals)
    // e.g. Set.of("io.magnesium.", "java.", "javax.")
    public ReloadClassLoader(@NotNull Path classesDir, ClassLoader parent, Set<String> parentPrefixes)
            throws MalformedURLException {
        super("magnesium-reload", new URL[]{ classesDir.toUri().toURL() }, parent);
        this.parentPrefixes = parentPrefixes;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // Always delegate framework + JDK classes to parent
        for (String parentPrefix : parentPrefixes) {
            if (name.startsWith(parentPrefix)) {
                return super.loadClass(name, resolve);
            }
        }

        // For user classes: check our own URL first (child-first)
        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);
            if (loaded != null) return loaded;

            try {
                Class<?> found = findClass(name);
                if (resolve) resolveClass(found);
                return found;
            } catch (ClassNotFoundException e) {
                return super.loadClass(name, resolve);
            }
        }
    }

    public void dispose() {
        try {
            close();
        } catch (IOException ignored) {}
    }
}