package net.magnesiumbackend.core.annotations;

import net.magnesiumbackend.core.annotations.enums.LifecycleStage;
import net.magnesiumbackend.core.lifecycle.LifecycleDefinition;
import net.magnesiumbackend.core.lifecycle.LifecycleGraph;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a component's lifecycle stage.
 *
 * <p>For dependencies, use the separate {@link DependsOn} annotation which supports:
 * <ul>
 *   <li>Multiple dependencies via repeatable annotation</li>
 *   <li>Optional dependencies for microservice resilience</li>
 *   <li>Per-dependency timeouts and retry configuration</li>
 *   <li>Graceful degradation when dependencies are unavailable</li>
 * </ul>
 *
 * <p>The annotation processor detects all @Lifecycle annotations at compile time
 * and builds a precomputed initialization graph. It validates for:
 * <ul>
 *   <li>Cyclic dependencies (reported as compilation errors)</li>
 *   <li>Invalid stage ordering</li>
 * </ul>
 * </p>
 *
 * <h3>Stage Ordering</h3>
 * <p>Components are initialized in stage order: PRE_INIT → INIT → POST_INIT → READY.
 * Within each stage, dependencies (declared via @DependsOn) are resolved before
dependent components.</p>
 *
 * <h3>Basic Usage</h3>
 * <pre>{@code
 * @RestService
 * @Lifecycle(stage = LifecycleStage.INIT)
 * @DependsOn(ConfigService.class)
 * public class DatabaseService {
 *     @OnInitialize
 *     void connect() { ... }
 * }
 * }</pre>
 *
 * <h3>Async Initialization</h3>
 * <pre>{@code
 * @RestService
 * @Lifecycle(stage = LifecycleStage.POST_INIT, async = true)
 * public class CacheService {
 *     @OnInitialize
 *     void warmUp() { ... }
 * }
 * }</pre>
 *
 * <h3>Microservice-Style with Optional Dependencies</h3>
 * <pre>{@code
 * @RestService
 * @Lifecycle(stage = LifecycleStage.READY)
 * @DependsOn(value = DatabaseService.class)                    // Required
 * @DependsOn(value = NotificationService.class, optional = true)  // Optional
 * public class OrderService {
 *     public OrderService(DatabaseService db,
 *                         @Inject(optional = true) NotificationService notifications) {
 *         // notifications may be null if service unavailable
 *     }
 * }
 * }</pre>
 *
 * <h3>Code-Driven Alternative</h3>
 * <p>For dynamic registration, use {@link LifecycleDefinition} with
 * {@link LifecycleGraph} for programmatic lifecycle management.</p>
 *
 * @see LifecycleStage
 * @see DependsOn
 * @see RestService
 * @see Inject
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
     * <p>Components are initialized in stage order:
     * PRE_INIT → INIT → POST_INIT → READY</p>
     *
     * @return the lifecycle stage
     */
    LifecycleStage stage();

    /**
     * Whether this component should be initialized asynchronously.
     * Async components in the same stage run in parallel after their
     * {@link DependsOn} dependencies complete.
     *
     * @return true for async initialization
     */
    boolean async() default false;
}