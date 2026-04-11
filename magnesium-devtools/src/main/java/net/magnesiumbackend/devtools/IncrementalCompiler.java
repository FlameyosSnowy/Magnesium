package net.magnesiumbackend.devtools;

import org.jetbrains.annotations.NotNull;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class IncrementalCompiler {
    private static final Pattern PATTERN = Pattern.compile("\\$.*\\.class$");
    private final Path sourceRoot;
    private final Path outputRoot;
    private final List<Path> classpath;

    public IncrementalCompiler(Path sourceRoot, Path outputRoot, List<Path> classpath) {
        this.sourceRoot = sourceRoot;
        this.outputRoot = outputRoot;
        this.classpath = classpath;
    }

    public CompileResult compile(@NotNull Set<Path> changedClasses) {
        Set<Path> sources = new HashSet<>(changedClasses.size());
        for (Path changedClass : changedClasses) {
            Path path = classToSource(changedClass);
            if (Files.exists(path)) {
                sources.add(path);
            }
        }

        if (sources.isEmpty()) return CompileResult.NOOP;
        return compileFiles(sources);
    }

    public CompileResult compileFiles(Set<Path> sourceFiles) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException(
            "No JavaCompiler available, are you running on a JDK, not a JRE?"
        );

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        try (StandardJavaFileManager fm = compiler.getStandardFileManager(diagnostics, null, null)) {
            fm.setLocation(StandardLocation.CLASS_OUTPUT, List.of(outputRoot.toFile()));
            List<File> list = new ArrayList<>(classpath.size());
            for (Path path : classpath) {
                File file = path.toFile();
                list.add(file);
            }
            fm.setLocation(StandardLocation.CLASS_PATH, list);

            Iterable<? extends JavaFileObject> units =
                fm.getJavaFileObjectsFromPaths(sourceFiles);

            boolean success = compiler.getTask(null, fm, diagnostics, 
                List.of("-g"), null, units).call();

            List<String> errors = new ArrayList<>(diagnostics.getDiagnostics().size());
            for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                if (d.getKind() == Diagnostic.Kind.ERROR) {
                    String string = d.toString();
                    errors.add(string);
                }
            }

            return new CompileResult(success, errors);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Path classToSource(Path classFile) {
        String rel = outputRoot.relativize(classFile).toString();
        String sourceName = PATTERN.matcher(rel).replaceAll(".java") // strip inner classes
            .replace(".class", ".java");
        return sourceRoot.resolve(sourceName);
    }

    public record CompileResult(boolean success, List<String> errors) {
        public static final CompileResult NOOP = new CompileResult(true, List.of());
    }
}