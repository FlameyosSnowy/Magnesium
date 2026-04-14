package net.magnesiumbackend.core.lifecycle;

import net.magnesiumbackend.core.annotations.enums.LifecycleStage;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Runtime registry for executing lifecycle phases.
 *
 * <p>LifecycleRegistry manages the execution of component initialization
 * across all lifecycle stages. It uses a precomputed {@link LifecycleGraph}
 * to ensure correct ordering and supports both synchronous and asynchronous
 * initialization.</p>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * LifecycleRegistry registry = new LifecycleRegistry();
 *
 * // Register components
 * registry.register(LifecycleDefinition.builder()
 *     .component(DatabaseService.class)
 *     .stage(LifecycleStage.INIT)
 *     .dependsOn(ConfigService.class)
 *     .onInitialize(db -> db.connect())
 *     .build());
 *
 * // Build and execute
 * LifecycleGraph graph = registry.buildGraph();
 * if (graph.hasErrors()) {
 *     graph.getErrors().forEach(System.err::println);
 *     return;
 * }
 *
 * registry.execute(
 *     componentClass -> serviceRegistry.get(componentClass),
 *     error -> System.err.println("Init failed: " + error)
 * );
 * }</pre>
 *
 * @see LifecycleGraph
 * @see LifecycleDefinition
 */
public final class LifecycleRegistry {

    private final List<LifecycleDefinition> definitions = new ArrayList<>();
    private LifecycleGraph graph;

    /**
     * Registers a lifecycle definition.
     *
     * @param definition the lifecycle definition to register
     * @return this registry
     */
    public LifecycleRegistry register(LifecycleDefinition definition) {
        Objects.requireNonNull(definition, "definition cannot be null");
        definitions.add(definition);
        this.graph = null; // Invalidate cached graph
        return this;
    }

    /**
     * Registers multiple lifecycle definitions.
     *
     * @param definitions the definitions to register
     * @return this registry
     */
    public LifecycleRegistry registerAll(Collection<LifecycleDefinition> definitions) {
        for (LifecycleDefinition def : definitions) {
            register(def);
        }
        return this;
    }

    /**
     * Builds the lifecycle graph from all registered definitions.
     *
     * @return the computed lifecycle graph
     */
    public LifecycleGraph buildGraph() {
        if (graph != null) {
            return graph;
        }

        LifecycleGraph.Builder builder = LifecycleGraph.builder();
        for (LifecycleDefinition def : definitions) {
            builder.add(def);
        }

        graph = builder.build();
        return graph;
    }

    /**
     * Executes the lifecycle with the given component resolver.
     *
     * @param resolver resolves component classes to instances
     * @param errorHandler handles initialization errors
     * @return future that completes when all stages finish
     */
    public CompletableFuture<Void> executeAsync(
            Function<Class<?>, Object> resolver,
            Consumer<Throwable> errorHandler) {

        LifecycleGraph graph = buildGraph();
        if (graph.hasErrors()) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.completeExceptionally(
                new IllegalStateException("Cannot execute lifecycle with errors: " + graph.getErrors())
            );
            return future;
        }

        List<Class<?>> ordered = graph.getOrderedComponents();
        Map<Class<?>, CompletableFuture<Void>> futures = new ConcurrentHashMap<>(16);

        // Process each stage in order
        CompletableFuture<Void> result = CompletableFuture.completedFuture(null);

        for (LifecycleStage stage : LifecycleStage.values()) {
            List<Class<?>> stageComponents = new ArrayList<>(ordered.size());
            for (Class<?> c : ordered) {
                if (graph.getNode(c).stage().equals(stage)) {
                    stageComponents.add(c);
                }
            }

            if (stageComponents.isEmpty()) {
                continue;
            }

            result = result.thenCompose(v -> executeStageAsync(stageComponents, graph, resolver, errorHandler, futures));
        }

        return result;
    }

    /**
     * Executes the lifecycle synchronously.
     *
     * @param resolver resolves component classes to instances
     * @param errorHandler handles initialization errors
     * @throws IllegalStateException if the lifecycle graph has errors
     */
    public void execute(Function<Class<?>, Object> resolver, Consumer<Throwable> errorHandler) {
        try {
            executeAsync(resolver, errorHandler).join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(cause);
        }
    }

    private CompletableFuture<Void> executeStageAsync(
            List<Class<?>> components,
            LifecycleGraph graph,
            Function<Class<?>, Object> resolver,
            Consumer<Throwable> errorHandler,
            Map<Class<?>, CompletableFuture<Void>> futures) {

        List<CompletableFuture<Void>> stageFutures = new ArrayList<>();

        for (Class<?> componentClass : components) {
            LifecycleGraph.Node node = graph.getNode(componentClass);

            // Wait for dependencies
            CompletableFuture<Void> depFuture = CompletableFuture.completedFuture(null);
            for (Class<?> dep : node.dependencies()) {
                CompletableFuture<Void> depResult = futures.get(dep);
                if (depResult != null) {
                    depFuture = depFuture.thenCombine(depResult, (a, b) -> null);
                }
            }

            // Execute this component
            CompletableFuture<Void> componentFuture = depFuture.thenCompose(v -> {
                Object instance = resolver.apply(componentClass);
                if (instance == null) {
                    return CompletableFuture.failedFuture(
                        new IllegalStateException("No instance found for: " + componentClass)
                    );
                }

                return executeComponentAsync(instance, node, errorHandler);
            });

            futures.put(componentClass, componentFuture);
            stageFutures.add(componentFuture);
        }

        return CompletableFuture.allOf(stageFutures.toArray(new CompletableFuture[0]));
    }

    private CompletableFuture<Void> executeComponentAsync(
            Object instance,
            LifecycleGraph.Node node,
            Consumer<Throwable> errorHandler) {

        // Find the definition for this component
        LifecycleDefinition found = null;
        for (LifecycleDefinition d : definitions) {
            if (d.component() == node.component()) {
                found = d;
                break;
            }
        }
        LifecycleDefinition definition = found;

        if (definition == null || definition.initializer() == null) {
            return CompletableFuture.completedFuture(null);
        }

        if (node.async()) {
            return CompletableFuture.runAsync(() -> {
                try {
                    definition.initializer().accept(instance);
                } catch (Throwable e) {
                    errorHandler.accept(e);
                    throw new CompletionException(e);
                }
            });
        } else {
            return CompletableFuture.runAsync(() -> {
                try {
                    definition.initializer().accept(instance);
                } catch (Throwable e) {
                    errorHandler.accept(e);
                    throw new CompletionException(e);
                }
            }, Runnable::run); // Execute synchronously on calling thread
        }
    }

    /**
     * Returns all registered definitions.
     *
     * @return unmodifiable list of definitions
     */
    public List<LifecycleDefinition> getDefinitions() {
        return Collections.unmodifiableList(definitions);
    }

    /**
     * Clears all registered definitions.
     *
     * @return this registry
     */
    public LifecycleRegistry clear() {
        definitions.clear();
        graph = null;
        return this;
    }
}
