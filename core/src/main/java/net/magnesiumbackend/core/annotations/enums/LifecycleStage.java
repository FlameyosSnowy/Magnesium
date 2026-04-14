package net.magnesiumbackend.core.annotations.enums;

import net.magnesiumbackend.core.annotations.Lifecycle;
import net.magnesiumbackend.core.lifecycle.LifecycleGraph;
import net.magnesiumbackend.core.lifecycle.LifecycleRegistry;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle stages for component initialization ordering.
 *
 * <p>Components are initialized in stage order: PRE_INIT → INIT → POST_INIT → READY.
 * Within each stage, dependencies are resolved before dependent components. Components
 * in earlier stages cannot depend on components in later stages.</p>
 *
 * <h3>Stages</h3>
 * <ul>
 *   <li><b>PRE_INIT</b> - Lowest-level infrastructure (configuration loaders, loggers)</li>
 *   <li><b>INIT</b> - Core services (database connections, thread pools)</li>
 *   <li><b>POST_INIT</b> - Dependent services (caches, background tasks)</li>
 *   <li><b>READY</b> - Final readiness checks and service advertisement</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * @Lifecycle(stage = LifecycleStage.INIT, dependsOn = ConfigLoader.class)
 * public class DatabaseService { }
 * }</pre>
 *
 * @see Lifecycle
 * @see LifecycleGraph
 * @see LifecycleRegistry
 */
public enum LifecycleStage {
    /**
     * Infrastructure initialization stage.
     * Used for configuration loaders, logging frameworks, and other
     * foundational services required by all other components.
     */
    PRE_INIT,

    /**
     * Core service initialization stage.
     * Used for database connections, HTTP clients, thread pools,
     * and other core infrastructure services.
     */
    INIT,

    /**
     * Post-initialization stage for dependent services.
     * Used for caches that depend on databases, background tasks,
     * and services that depend on core infrastructure.
     */
    POST_INIT,

    /**
     * Final readiness stage.
     * Used for health checks, service advertisement, metrics export,
     * and any final setup that requires all other services to be ready.
     */
    READY;

    public static final Set<LifecycleStage> LIFECYCLE_STAGES = Collections.unmodifiableSet(EnumSet.allOf(LifecycleStage.class));
}