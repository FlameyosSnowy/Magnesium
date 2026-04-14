package net.magnesiumbackend.processor.generator;

import net.magnesiumbackend.core.annotations.Lifecycle;
import net.magnesiumbackend.core.annotations.OnInitialize;
import net.magnesiumbackend.core.annotations.enums.LifecycleStage;
import net.magnesiumbackend.core.lifecycle.LifecycleDefinition;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * Generates lifecycle metadata and validates lifecycle dependencies at compile time.
 *
 * <p>This generator:
 * <ul>
 *   <li>Processes @Lifecycle annotations on classes</li>
 *   <li>Builds dependency graph and detects cycles</li>
 *   <li>Generates lifecycle registry code</li>
 *   <li>Reports errors for invalid configurations</li>
 * </ul>
 * </p>
 */
public class LifecycleRegistrationGenerator {

    private final Types types;
    private final Filer filer;
    private final Elements elements;
    private final Messager messager;
    private final Map<String, LifecycleMetadata> metadata = new HashMap<>();
    private final Map<String, String> initializers = new HashMap<>(); // className -> methodName

    public LifecycleRegistrationGenerator(Types types, Filer filer, Elements elements, Messager messager) {
        this.types = types;
        this.filer = filer;
        this.elements = elements;
        this.messager = messager;
    }

    /**
     * Processes a type element annotated with @Lifecycle.
     *
     * @param element the annotated type element
     * @return true if processing succeeded, false if errors occurred
     */
    public boolean processLifecycle(TypeElement element) {
        if (element.getKind() != ElementKind.CLASS) {
            error("@Lifecycle can only be applied to classes", element);
            return false;
        }

        Lifecycle annotation = element.getAnnotation(Lifecycle.class);
        if (annotation == null) {
            return false;
        }

        String className = element.getQualifiedName().toString();
        LifecycleStage stage = annotation.stage();
        boolean async = annotation.async();

        // Extract dependencies from annotation
        List<String> dependencies = extractDependencies(annotation);

        // Validate no self-dependency
        for (String dep : dependencies) {
            if (dep.equals(className)) {
                error("Self-dependency detected: " + className, element);
                return false;
            }
        }

        // Look for @OnInitialize method
        String initializerMethod = findOnInitializeMethod(element);
        if (initializerMethod != null) {
            initializers.put(className, initializerMethod);
        }

        LifecycleMetadata data = new LifecycleMetadata(className, stage, dependencies, async);
        metadata.put(className, data);

        return true;
    }

    /**
     * Finds the @OnInitialize method in the given class.
     *
     * @param element the class to search
     * @return the method name, or null if not found
     */
    private String findOnInitializeMethod(TypeElement element) {
        String found = null;

        for (Element enclosed : element.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.METHOD) continue;

            ExecutableElement method = (ExecutableElement) enclosed;
            OnInitialize annotation = method.getAnnotation(OnInitialize.class);
            if (annotation == null) continue;

            if (found != null) {
                error("Multiple @OnInitialize methods found. Only one allowed per class.", element);
                return found;
            }

            // Validate method signature
            if (!method.getParameters().isEmpty()) {
                error("@OnInitialize method must have no parameters", method);
                continue;
            }

            if (method.getModifiers().contains(Modifier.STATIC)) {
                error("@OnInitialize method must not be static", method);
                continue;
            }

            if (method.getModifiers().contains(Modifier.PRIVATE)) {
                error("@OnInitialize method must not be private", method);
                continue;
            }

            found = method.getSimpleName().toString();
        }

        return found;
    }

    /**
     * Validates the complete lifecycle graph and reports any errors.
     *
     * @return list of error messages, empty if valid
     */
    public List<String> validateGraph() {
        List<String> errors = new ArrayList<>();

        // Check for missing dependencies
        for (LifecycleMetadata data : metadata.values()) {
            for (String dep : data.dependencies) {
                if (!metadata.containsKey(dep)) {
                    errors.add("Missing dependency: " + dep + " required by " + data.className);
                }
            }
        }

        // Detect cycles using DFS
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (String className : metadata.keySet()) {
            if (!visited.contains(className)) {
                List<String> cycle = detectCycle(className, visited, recursionStack, new ArrayList<>());
                if (cycle != null) {
                    errors.add("Cyclic dependency detected: " + formatCycle(cycle));
                    break; // Report first cycle found
                }
            }
        }

        // Validate stage ordering (dependencies should be same or earlier stage)
        for (LifecycleMetadata data : metadata.values()) {
            for (String dep : data.dependencies) {
                LifecycleMetadata depData = metadata.get(dep);
                if (depData != null && depData.stage.ordinal() > data.stage.ordinal()) {
                    errors.add("Invalid stage ordering: " + data.className +
                              " (" + data.stage + ") depends on " + dep +
                              " (" + depData.stage + ") which is a later stage");
                }
            }
        }

        return errors;
    }

    /**
     * Generates the lifecycle registration code.
     *
     * @return the fully qualified name of the generated class, or null if generation failed
     */
    public String generate() {
        List<String> errors = validateGraph();
        if (!errors.isEmpty()) {
            for (String error : errors) {
                messager.printMessage(javax.tools.Diagnostic.Kind.ERROR, error);
            }
            return null;
        }

        if (metadata.isEmpty()) {
            return null;
        }
        

        // Compute topological order before resource creation
        List<LifecycleMetadata> ordered = computeTopologicalOrder();

        try {
            String packageName = "net.magnesiumbackend.core.lifecycle.generated";
            String className = "LifecycleRegistration" + System.currentTimeMillis();
            String fqn = packageName + "." + className;

            FileObject file = filer.createResource(
                StandardLocation.CLASS_OUTPUT,
                packageName,
                className + ".txt"
            );

            try (Writer writer = file.openWriter()) {
                writer.write("# Lifecycle Registration\n");
                writer.write("# Generated at: " + new Date() + "\n\n");

                // Write components in topological order
                for (LifecycleMetadata data : ordered) {
                    writer.write("component=" + data.className + "\n");
                    writer.write("stage=" + data.stage + "\n");
                    writer.write("async=" + data.async + "\n");
                    writer.write("dependencies=" + String.join(",", data.dependencies) + "\n");
                    writer.write("\n");
                }

            }
            generateJavaSource(packageName, className, ordered);

            return fqn;
        } catch (IOException e) {
            messager.printMessage(javax.tools.Diagnostic.Kind.ERROR,
                "Failed to generate lifecycle registration: " + e.getMessage());
            return null;
        }
    }

    private void generateJavaSource(String packageName, String className,
                                     List<LifecycleMetadata> ordered) throws IOException {
        String javaClassName = className + "Loader";

        FileObject sourceFile = filer.createSourceFile(
            packageName + "." + javaClassName
        );

        try (Writer w = sourceFile.openWriter()) {
            w.write("package " + packageName + ";\n\n");
            w.write("import net.magnesiumbackend.core.lifecycle.LifecycleDefinition;\n");
            w.write("import net.magnesiumbackend.core.lifecycle.LifecycleRegistry;\n");
            w.write("import net.magnesiumbackend.core.annotations.enums.LifecycleStage;\n\n");
            w.write("import java.util.List;\n\n");

            w.write("/**\n");
            w.write(" * Generated lifecycle registration loader.\n");
            w.write(" * This class is generated at compile time based on @Lifecycle annotations.\n");
            w.write(" */\n");
            w.write("public final class " + javaClassName + " {\n\n");

            w.write("    /**\n");
            w.write("     * Returns all lifecycle definitions in dependency order.\n");
            w.write("     * @return list of lifecycle definitions\n");
            w.write("     */\n");
            w.write("    public static List<LifecycleDefinition> getDefinitions() {\n");
            w.write("        return List.of(\n");

            for (int i = 0; i < ordered.size(); i++) {
                LifecycleMetadata data = ordered.get(i);
                w.write("            LifecycleDefinition.builder()\n");
                w.write("                .component(" + data.className + ".class)\n");
                w.write("                .stage(LifecycleStage." + data.stage + ")\n");
                if (!data.dependencies.isEmpty()) {
                    w.write("                .dependsOn(\n");
                    for (int j = 0; j < data.dependencies.size(); j++) {
                        w.write("                    " + data.dependencies.get(j) + ".class");
                        if (j < data.dependencies.size() - 1) {
                            w.write(",");
                        }
                        w.write("\n");
                    }
                    w.write("                )\n");
                }
                w.write("                .async(" + data.async + ")\n");

                // Add initializer if present
                String initMethod = initializers.get(data.className);
                if (initMethod != null) {
                    w.write("                .onInitialize(instance -> ((" + data.className + ") instance)." + initMethod + "())\n");
                }

                w.write("                .build()");
                if (i < ordered.size() - 1) {
                    w.write(",");
                }
                w.write("\n");
            }

            w.write("        );\n");
            w.write("    }\n\n");

            w.write("    /**\n");
            w.write("     * Registers all lifecycle definitions with the given registry.\n");
            w.write("     * @param registry the lifecycle registry\n");
            w.write("     */\n");
            w.write("    public static void registerAll(LifecycleRegistry registry) {\n");
            w.write("        getDefinitions().forEach(registry::register);\n");
            w.write("    }\n\n");

            w.write("    private " + javaClassName + "() {}\n");
            w.write("}\n");
        }
    }

    private List<String> extractDependencies(Lifecycle annotation) {
        List<String> deps = new ArrayList<>();

        try {
            annotation.dependsOn(); // This throws MirroredTypeException
        } catch (MirroredTypeException mte) {
            // Single dependency case (unlikely but handle it)
            TypeMirror typeMirror = mte.getTypeMirror();
            if (typeMirror != null) {
                deps.add(typeMirror.toString());
            }
        } catch (IllegalArgumentException e) {
            // Multiple dependencies - use the class names directly
            for (Class<?> depClass : annotation.dependsOn()) {
                deps.add(depClass.getCanonicalName());
            }
        }

        return deps;
    }

    private List<String> detectCycle(String current, Set<String> visited,
                                     Set<String> recursionStack, List<String> path) {
        visited.add(current);
        recursionStack.add(current);
        path.add(current);

        LifecycleMetadata data = metadata.get(current);
        if (data != null) {
            for (String dep : data.dependencies) {
                if (!metadata.containsKey(dep)) {
                    continue; // Missing dependency, handled elsewhere
                }

                if (!visited.contains(dep)) {
                    List<String> cycle = detectCycle(dep, visited, recursionStack, path);
                    if (cycle != null) {
                        return cycle;
                    }
                } else if (recursionStack.contains(dep)) {
                    // Found cycle - extract cycle from path
                    int cycleStart = path.indexOf(dep);
                    return new ArrayList<>(path.subList(cycleStart, path.size()));
                }
            }
        }

        path.removeLast();
        recursionStack.remove(current);
        return null;
    }

    private String formatCycle(List<String> cycle) {
        StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < cycle.size(); i++) {
            if (i > 0) sb.append(" -> ");
            String className = cycle.get(i);
            int lastDot = className.lastIndexOf('.');
            sb.append(lastDot > 0 ? className.substring(lastDot + 1) : className);
        }
        sb.append(" -> ...");
        return sb.toString();
    }

    private List<LifecycleMetadata> computeTopologicalOrder() {
        Collection<LifecycleMetadata> values = metadata.values();
        Map<String, Integer> inDegree = new HashMap<>(values.size());
        Map<LifecycleStage, List<String>> byStage = new EnumMap<>(LifecycleStage.class);

        // Initialize
        for (LifecycleMetadata data : values) {
            inDegree.put(data.className, data.dependencies.size());
            byStage.computeIfAbsent(data.stage, k -> new ArrayList<>(16)).add(data.className);
        }

        Set<LifecycleStage> stages = LifecycleStage.LIFECYCLE_STAGES;
        List<LifecycleMetadata> result = new ArrayList<>(16);

        // Process stages in order
        for (LifecycleStage stage : stages) {
            List<String> stageComponents = byStage.getOrDefault(stage, List.of());
            Queue<String> queue = new LinkedList<>();

            for (String className : stageComponents) {
                if (inDegree.getOrDefault(className, 0) == 0) {
                    queue.offer(className);
                }
            }

            while (!queue.isEmpty()) {
                String current = queue.poll();
                result.add(metadata.get(current));

                // Reduce in-degree for dependents
                for (LifecycleMetadata dependent : values) {
                    if (dependent.dependencies.contains(current)) {
                        int newDegree = inDegree.get(dependent.className) - 1;
                        inDegree.put(dependent.className, newDegree);

                        LifecycleStage dependentStage = metadata.get(dependent.className).stage;
                        if (newDegree == 0 && dependentStage.ordinal() >= stage.ordinal()) {
                            queue.offer(dependent.className);
                        }
                    }
                }
            }
        }

        return result;
    }

    private void error(String message, Element element) {
        messager.printMessage(javax.tools.Diagnostic.Kind.ERROR, message, element);
    }

    /**
     * Internal record for lifecycle metadata.
     */
    record LifecycleMetadata(
        String className,
        LifecycleStage stage,
        List<String> dependencies,
        boolean async
    ) {}
}
