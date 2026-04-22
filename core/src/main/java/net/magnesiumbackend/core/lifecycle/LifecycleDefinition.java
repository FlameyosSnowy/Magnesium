package net.magnesiumbackend.core.lifecycle;

import net.magnesiumbackend.core.annotations.enums.LifecycleStage;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Code-driven lifecycle definition for programmatic lifecycle management.
 *
 * <p>LifecycleDefinition provides an object-oriented alternative to the
 * {@code @Lifecycle} annotation for defining component lifecycle metadata.
 * Use this when you need dynamic or conditional lifecycle registration.</p>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * LifecycleDefinition def = LifecycleDefinition.builder()
 *     .component(DatabaseService.class)
 *     .stage(LifecycleStage.INIT)
 *     .dependsOn(ConfigLoader.class, LoggerFactory.class)
 *     .async(false)
 *     .onInitialize(service -> service.connect())
 *     .build();
 *
 * registry.register(def);
 * }</pre>
 *
 * @see LifecycleGraph
 * @see LifecycleRegistry
 * @see net.magnesiumbackend.core.annotations.Lifecycle
 */
public final class LifecycleDefinition {

    private final Class<?> component;
    private final LifecycleStage stage;
    private final Set<Class<?>> dependencies;
    private final boolean async;
    private final Consumer<Object> initializer;

    private LifecycleDefinition(Builder builder) {
        this.component = Objects.requireNonNull(builder.component, "component cannot be null");
        this.stage = Objects.requireNonNull(builder.stage, "stage cannot be null");
        this.dependencies = Set.copyOf(builder.dependencies);
        this.async = builder.async;
        this.initializer = builder.initializer;
    }

    public LifecycleDefinition(LifecycleStage stage, Class<?> component, Set<Class<?>> dependencies, boolean async, Consumer<Object> initializer) {
        this.stage = stage;
        this.component = component;
        this.dependencies = dependencies;
        this.async = async;
        this.initializer = initializer;
    }

    public LifecycleDefinition(LifecycleStage stage, Class<?> component, Consumer<Object> initializer) {
        this(stage, component, new HashSet<>(), false, initializer);
    }

    /**
     * Returns the component class this lifecycle applies to.
     *
     * @return the component class
     */
    public Class<?> component() {
        return component;
    }

    /**
     * Returns the lifecycle stage for this component.
     *
     * @return the lifecycle stage
     */
    public LifecycleStage stage() {
        return stage;
    }

    /**
     * Returns the set of components this one depends on.
     *
     * @return unmodifiable set of dependency classes
     */
    public Set<Class<?>> dependencies() {
        return dependencies;
    }

    /**
     * Returns true if this component should be initialized asynchronously.
     *
     * @return true for async initialization
     */
    public boolean isAsync() {
        return async;
    }

    /**
     * Returns the initializer action, or null if none.
     *
     * @return the initializer consumer
     */
    public Consumer<Object> initializer() {
        return initializer;
    }

    /**
     * Creates a new builder for lifecycle definition.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating LifecycleDefinition instances.
     */
    public static final class Builder {
        private Class<?> component;
        private LifecycleStage stage = LifecycleStage.INIT;
        private final Set<Class<?>> dependencies = new HashSet<>();
        private boolean async = false;
        private Consumer<Object> initializer;

        private Builder() {}

        /**
         * Sets the component class.
         *
         * @param component the component class
         * @return this builder
         */
        public Builder component(Class<?> component) {
            this.component = component;
            return this;
        }

        /**
         * Sets the lifecycle stage.
         *
         * @param stage the lifecycle stage
         * @return this builder
         */
        public Builder stage(LifecycleStage stage) {
            this.stage = stage;
            return this;
        }

        /**
         * Adds dependencies for this component.
         *
         * @param dependencies the dependency classes
         * @return this builder
         */
        public Builder dependsOn(Class<?>... dependencies) {
            this.dependencies.addAll(Arrays.asList(dependencies));
            return this;
        }

        /**
         * Sets whether this component should be initialized asynchronously.
         *
         * @param async true for async initialization
         * @return this builder
         */
        public Builder async(boolean async) {
            this.async = async;
            return this;
        }

        /**
         * Sets the initializer action to run during lifecycle execution.
         *
         * @param initializer the initialization consumer
         * @return this builder
         */
        public Builder onInitialize(Consumer<Object> initializer) {
            this.initializer = initializer;
            return this;
        }

        /**
         * Builds the lifecycle definition.
         *
         * @return a new LifecycleDefinition instance
         * @throws NullPointerException if component or stage is not set
         */
        public LifecycleDefinition build() {
            return new LifecycleDefinition(this);
        }
    }
}
