package net.magnesiumbackend.core.lifecycle;

import net.magnesiumbackend.core.annotations.enums.LifecycleStage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Precomputed lifecycle dependency graph with cycle detection.
 *
 * <p>LifecycleGraph builds a topological ordering of components based on their
 * dependencies and lifecycle stages. It detects cycles and reports errors
 * during construction. The graph supports both annotation-driven and
 * code-driven lifecycle definitions.</p>
 *
 * <h3>Stage Ordering</h3>
 * <p>Components are ordered first by stage (PRE_INIT → INIT → POST_INIT → READY),
 * then by dependencies within each stage.</p>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * LifecycleGraph graph = LifecycleGraph.builder()
 *     .add(DatabaseService.class, LifecycleStage.INIT,
 *          Set.of(ConfigLoader.class, LoggerFactory.class), false)
 *     .add(CacheService.class, LifecycleStage.POST_INIT,
 *          Set.of(DatabaseService.class), true)
 *     .build();
 *
 * List<Class<?>> order = graph.getOrderedComponents();
 * }</pre>
 *
 * @see LifecycleDefinition
 * @see LifecycleRegistry
 */
public final class LifecycleGraph {

    private final Map<Class<?>, Node> nodes;
    private final List<Class<?>> orderedComponents;
    private final List<String> errors;

    private LifecycleGraph(Builder builder) {
        this.nodes = new ConcurrentHashMap<>(builder.nodes);
        this.errors = List.copyOf(builder.errors);

        if (!errors.isEmpty()) {
            this.orderedComponents = List.of();
        } else {
            this.orderedComponents = computeTopologicalOrder();
        }
    }

    /**
     * Returns the ordered list of components for lifecycle execution.
     *
     * @return unmodifiable list of component classes in execution order
     * @throws IllegalStateException if the graph has errors
     */
    public List<Class<?>> getOrderedComponents() {
        if (!errors.isEmpty()) {
            throw new IllegalStateException(
                "Cannot get ordered components: " + String.join(", ", errors)
            );
        }
        return orderedComponents;
    }

    /**
     * Returns any errors detected during graph construction.
     *
     * @return unmodifiable list of error messages
     */
    public List<String> getErrors() {
        return errors;
    }

    /**
     * Returns true if the graph has errors (cycles, missing dependencies, etc.).
     *
     * @return true if errors exist
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Returns the node for a component, or null if not present.
     *
     * @param component the component class
     * @return the node, or null
     */
    public Node getNode(Class<?> component) {
        return nodes.get(component);
    }

    /**
     * Returns all nodes in the graph.
     *
     * @return unmodifiable collection of nodes
     */
    public Collection<Node> getNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    /**
     * Computes topological order using Kahn's algorithm with stage grouping.
     */
    private List<Class<?>> computeTopologicalOrder() {
        List<Class<?>> result = new ArrayList<>(nodes.size());
        Map<Class<?>, Integer> inDegree = new HashMap<>();
        Map<LifecycleStage, List<Class<?>>> byStage = new EnumMap<>(LifecycleStage.class);

        // Initialize in-degrees and stage groups
        for (Node node : nodes.values()) {
            inDegree.put(node.component, node.dependencies.size());
            byStage.computeIfAbsent(node.stage, k -> new ArrayList<>()).add(node.component);
        }

        // Process stages in order
        for (LifecycleStage stage : LifecycleStage.values()) {
            List<Class<?>> stageComponents = byStage.getOrDefault(stage, List.of());
            Queue<Class<?>> queue = new LinkedList<>();

            // Find nodes with no remaining dependencies in this stage or earlier
            for (Class<?> component : stageComponents) {
                if (inDegree.getOrDefault(component, 0) == 0) {
                    queue.offer(component);
                }
            }

            while (!queue.isEmpty()) {
                Class<?> current = queue.poll();
                result.add(current);

                // Reduce in-degree for dependents
                for (Node node : nodes.values()) {
                    if (node.dependencies.contains(current)) {
                        int newDegree = inDegree.get(node.component) - 1;
                        inDegree.put(node.component, newDegree);

                        // Only add to queue if in same or later stage and no more deps
                        if (newDegree == 0 && node.stage.ordinal() >= stage.ordinal()) {
                            queue.offer(node.component);
                        }
                    }
                }
            }
        }

        // Check for remaining nodes (cycle or unresolved deps)
        if (result.size() != nodes.size()) {
            Set<Class<?>> remaining = new HashSet<>(nodes.keySet());
            remaining.removeAll(result);

            // Try to detect specific cycle
            for (Class<?> component : remaining) {
                List<Class<?>> cycle = detectCycle(component, new HashSet<>(), new ArrayList<>());
                if (cycle != null) {
                    throw new IllegalStateException(
                        "Cyclic dependency detected: " + formatCycle(cycle)
                    );
                }
            }

            throw new IllegalStateException(
                "Unresolved dependencies for: " + remaining
            );
        }

        return Collections.unmodifiableList(result);
    }

    /**
     * Detects a cycle starting from the given component using DFS.
     */
    private List<Class<?>> detectCycle(Class<?> start, Set<Class<?>> visited, List<Class<?>> path) {
        if (path.contains(start)) {
            int cycleStart = path.indexOf(start);
            return new ArrayList<>(path.subList(cycleStart, path.size()));
        }

        if (visited.contains(start)) {
            return null;
        }

        visited.add(start);
        path.add(start);

        Node node = nodes.get(start);
        if (node != null) {
            for (Class<?> dep : node.dependencies) {
                List<Class<?>> cycle = detectCycle(dep, visited, path);
                if (cycle != null) {
                    return cycle;
                }
            }
        }

        path.remove(path.size() - 1);
        return null;
    }

    private String formatCycle(List<Class<?>> cycle) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cycle.size(); i++) {
            if (i > 0) sb.append(" → ");
            sb.append(cycle.get(i).getSimpleName());
        }
        sb.append(" → ").append(cycle.get(0).getSimpleName());
        return sb.toString();
    }

    /**
     * Creates a new builder for constructing the lifecycle graph.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Node in the lifecycle graph representing a component.
     */
    public record Node(
        Class<?> component,
        LifecycleStage stage,
        Set<Class<?>> dependencies,
        boolean async
    ) {
        /**
         * Returns true if this component depends on the given component.
         *
         * @param other the potential dependency
         * @return true if this component depends on other
         */
        public boolean dependsOn(Class<?> other) {
            return dependencies.contains(other);
        }
    }

    /**
     * Builder for constructing LifecycleGraph instances.
     */
    public static final class Builder {
        private final Map<Class<?>, Node> nodes = new HashMap<>();
        private final List<String> errors = new ArrayList<>();

        private Builder() {}

        /**
         * Adds a component to the graph.
         *
         * @param component    the component class
         * @param stage        the lifecycle stage
         * @param dependencies the set of dependencies
         * @param async        whether initialization is async
         * @return this builder
         */
        public Builder add(Class<?> component, LifecycleStage stage,
                          Set<Class<?>> dependencies, boolean async) {
            Objects.requireNonNull(component, "component cannot be null");
            Objects.requireNonNull(stage, "stage cannot be null");
            Objects.requireNonNull(dependencies, "dependencies cannot be null");

            if (nodes.containsKey(component)) {
                errors.add("Duplicate lifecycle definition for: " + component.getName());
                return this;
            }

            // Validate dependencies exist or will exist
            for (Class<?> dep : dependencies) {
                if (dep == component) {
                    errors.add("Self-dependency detected for: " + component.getName());
                }
            }

            nodes.put(component, new Node(component, stage, Set.copyOf(dependencies), async));
            return this;
        }

        /**
         * Adds a component from a LifecycleDefinition.
         *
         * @param definition the lifecycle definition
         * @return this builder
         */
        public Builder add(LifecycleDefinition definition) {
            return add(
                definition.component(),
                definition.stage(),
                definition.dependencies(),
                definition.isAsync()
            );
        }

        /**
         * Adds an error message to be reported during build.
         *
         * @param error the error message
         * @return this builder
         */
        public Builder error(String error) {
            errors.add(error);
            return this;
        }

        /**
         * Builds the lifecycle graph.
         *
         * @return a new LifecycleGraph instance
         */
        public LifecycleGraph build() {
            // Validate all dependencies reference known components
            for (Node node : nodes.values()) {
                for (Class<?> dep : node.dependencies) {
                    if (!nodes.containsKey(dep)) {
                        errors.add("Missing dependency: " + dep.getName() +
                                  " required by " + node.component.getName());
                    }
                }
            }

            return new LifecycleGraph(this);
        }
    }
}
