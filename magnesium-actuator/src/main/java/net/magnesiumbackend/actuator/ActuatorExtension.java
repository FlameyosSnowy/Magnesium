package net.magnesiumbackend.actuator;

import net.magnesiumbackend.actuator.health.DiskSpaceHealthIndicator;
import net.magnesiumbackend.actuator.health.JvmHealthIndicator;
import net.magnesiumbackend.actuator.metrics.MetricsRegistry;
import net.magnesiumbackend.core.MagnesiumRuntime;
import net.magnesiumbackend.core.extensions.MagnesiumExtension;
import net.magnesiumbackend.core.health.CompositeHealthIndicator;
import net.magnesiumbackend.core.health.HealthIndicatorContributor;
import net.magnesiumbackend.core.json.JsonProvider;
import org.jetbrains.annotations.NotNull;

import java.util.ServiceLoader;
import java.util.logging.Logger;

/**
 * Actuator extension for Magnesium framework.
 *
 * <p>Provides production-ready health checks and metrics endpoints:</p>
 * <ul>
 *   <li><code>GET /actuator/health</code> - Application health status</li>
 *   <li><code>GET /actuator/health/liveness</code> - Kubernetes liveness probe</li>
 *   <li><code>GET /actuator/health/readiness</code> - Kubernetes readiness probe</li>
 *   <li><code>GET /actuator/metrics</code> - Application metrics</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // With defaults (enabled automatically via ServiceLoader)
 * MagnesiumApplication.run(MyApp.class);
 *
 * // Custom configuration
 * runtime.services(s -> s
 *     .register(ActuatorConfig.class, ctx -> ActuatorConfig.builder()
 *         .basePath("/admin")
 *         .enableDiskHealth(false)
 *         .build()));
 * }</pre>
 *
 * <h3>Adding Custom Health Indicators</h3>
 * <pre>{@code
 * CompositeHealthIndicator health = ctx.get(CompositeHealthIndicator.class);
 * health.add(new DatabaseHealthIndicator(pool));
 * }</pre>
 */
public final class ActuatorExtension implements MagnesiumExtension {

    private static final Logger logger = Logger.getLogger(ActuatorExtension.class.getName());

    @Override
    public void configure(@NotNull MagnesiumRuntime runtime) {
        runtime.services(services -> {
            // Register configuration
            services.register(ActuatorConfig.class, ctx -> {
                try {
                    return ctx.configurationManager().get(ActuatorConfig.class);
                } catch (Exception e) {
                    return ActuatorConfig.defaults();
                }
            });

            // Register composite health indicator
            services.register(CompositeHealthIndicator.class, ctx -> {
                ActuatorConfig config = ctx.get(ActuatorConfig.class);
                CompositeHealthIndicator composite = new CompositeHealthIndicator();

                // Add built-in indicators based on config
                if (config.enableJvmHealth()) {
                    composite.add(new JvmHealthIndicator());
                    logger.info("Added JVM health indicator");
                }

                if (config.enableDiskHealth()) {
                    composite.add(new DiskSpaceHealthIndicator());
                    logger.info("Added disk space health indicator");
                }

                // Load health indicator contributors from extensions
                ServiceLoader<HealthIndicatorContributor> contributors = ServiceLoader.load(HealthIndicatorContributor.class);
                for (HealthIndicatorContributor contributor : contributors) {
                    try {
                        contributor.contribute(composite, runtime);
                        logger.info("Added health indicators from: " + contributor.getClass().getSimpleName());
                    } catch (Exception e) {
                        logger.warning("Failed to load health contributor: " + contributor.getClass().getName() + " - " + e.getMessage());
                    }
                }

                return composite;
            });

            // Register metrics registry
            services.register(MetricsRegistry.class, ctx -> {
                MetricsRegistry registry = new MetricsRegistry("app.");

                // Register JVM metrics
                registry.gauge("jvm.memory.heap.used",
                    () -> Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
                registry.gauge("jvm.memory.heap.max",
                    () -> Runtime.getRuntime().maxMemory());
                registry.gauge("jvm.threads.live",
                    Thread::activeCount);

                return registry;
            });

            // Register actuator controller
            services.register(ActuatorController.class, ctx -> {
                CompositeHealthIndicator health = ctx.get(CompositeHealthIndicator.class);
                MetricsRegistry metrics = ctx.get(MetricsRegistry.class);
                JsonProvider json = ctx.jsonProvider();
                ActuatorConfig config = ctx.get(ActuatorConfig.class);

                return new ActuatorController(health, metrics, json, config);
            });
        });

        // Register routes
        runtime.router()
            .get(basePath(runtime) + "/health", ctx -> {
                ActuatorController controller = runtime.serviceRegistry()
                    .get(ActuatorController.class);
                return controller.health();
            })
            .commit()

            .get(basePath(runtime) + "/health/liveness", ctx -> {
                ActuatorController controller = runtime.serviceRegistry()
                    .get(ActuatorController.class);
                return controller.liveness();
            })
            .commit()

            .get(basePath(runtime) + "/health/readiness", ctx -> {
                ActuatorController controller = runtime.serviceRegistry()
                    .get(ActuatorController.class);
                return controller.readiness();
            })
            .commit()

            .get(basePath(runtime) + "/metrics", ctx -> {
                ActuatorController controller = runtime.serviceRegistry()
                    .get(ActuatorController.class);
                return controller.metrics();
            })
            .commit();

        logger.info("Actuator endpoints registered at " + basePath(runtime));
    }

    private String basePath(MagnesiumRuntime runtime) {
        try {
            ActuatorConfig config = runtime.serviceRegistry().get(ActuatorConfig.class);
            return config.basePath();
        } catch (Exception e) {
            return "/actuator";
        }
    }

    @Override
    public @NotNull String name() {
        return "actuator";
    }
}
