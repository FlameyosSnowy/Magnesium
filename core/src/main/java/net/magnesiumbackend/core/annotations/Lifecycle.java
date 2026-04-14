package net.magnesiumbackend.core.annotations;

import net.magnesiumbackend.core.annotations.enums.LifecycleStage;
import net.magnesiumbackend.core.lifecycle.LifecycleDefinition;
import net.magnesiumbackend.core.lifecycle.LifecycleGraph;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a component's lifecycle stage and dependencies.
 *
 * <p>The annotation processor detects all @Lifecycle annotations at compile time
 * and builds a precomputed dependency graph. It validates the graph for:
 * <ul>
 *   <li>Cyclic dependencies (reported as compilation errors)</li>
 *   <li>Missing dependencies (reported as compilation errors)</li>
 *   <li>Invalid stage ordering (dependencies must be same or earlier stage)</li>
 * </ul>
 * </p>
 *
 * <h3>Stage Ordering</h3>
 * <p>Components are initialized in stage order: PRE_INIT → INIT → POST_INIT → READY.
 * Within each stage, dependencies are resolved before dependent components.</p>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * @Lifecycle(stage = LifecycleStage.INIT, dependsOn = {ConfigService.class})
 * public class DatabaseService {
 *     @OnInitialize
 *     void connect() { ... }
 * }
 *
 * @Lifecycle(stage = LifecycleStage.POST_INIT, async = true)
 * public class CacheService {
 *     @OnInitialize
 *     void warmUp() { ... }
 * }
 * }</pre>
 *
 * <h3>Code-Driven Alternative</h3>
 * <p>For dynamic registration, use {@link LifecycleDefinition} with
 * {@link LifecycleGraph} for programmatic lifecycle management.</p>
 *
 * @see LifecycleStage
 * @see LifecycleDefinition
 * @see LifecycleGraph
 * @see OnInitialize
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Lifecycle {

    /**
     * The lifecycle stage for this component.
     *
     * @return the lifecycle stage
     */
    LifecycleStage stage();

    /**
     * Components this one depends on.
     *
     * @return array of dependency classes
     */
    Class<?>[] dependsOn() default {};

    /**
     * Whether this component should be initialized asynchronously.
     * Async components in the same stage run in parallel after dependencies complete.
     *
     * @return true for async initialization
     */
    boolean async() default false;
}