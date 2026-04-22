package net.magnesiumbackend.actuator;

import net.magnesiumbackend.actuator.metrics.MetricsRegistry;
import net.magnesiumbackend.core.health.CompositeHealthIndicator;
import net.magnesiumbackend.core.health.Health;
import net.magnesiumbackend.core.health.HealthStatus;
import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.json.JsonProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Controller for actuator endpoints.
 *
 * <p>Provides:</p>
 * <ul>
 *   <li><code>/health</code> - Application health status</li>
 *   <li><code>/metrics</code> - Application metrics</li>
 * </ul>
 */
public final class ActuatorController {

    private final CompositeHealthIndicator healthIndicator;
    private final MetricsRegistry metricsRegistry;
    private final JsonProvider jsonProvider;
    private final ActuatorConfig config;

    /**
     * Creates a controller with the given configuration.
     *
     * @param healthIndicator the health indicator to use
     * @param metricsRegistry the metrics registry to use
     * @param jsonProvider the JSON provider for serialization
     * @param config the actuator configuration
     */
    public ActuatorController(
        @NotNull CompositeHealthIndicator healthIndicator,
        @NotNull MetricsRegistry metricsRegistry,
        @NotNull JsonProvider jsonProvider,
        @NotNull ActuatorConfig config
    ) {
        this.healthIndicator = healthIndicator;
        this.metricsRegistry = metricsRegistry;
        this.jsonProvider = jsonProvider;
        this.config = config;
    }

    /**
     * GET /health - Returns application health status.
     *
     * @return health response
     */
    public @NotNull ResponseEntity<?> health() {
        Health health = healthIndicator.health();

        int statusCode = switch (health.status()) {
            case UP -> 200;
            case UNKNOWN -> 200;
            case OUT_OF_SERVICE -> 503;
            case DOWN -> 503;
        };

        Map<String, Object> body = Map.of(
            "status", health.status().code(),
            "details", health.details()
        );

        return ResponseEntity.of(statusCode, jsonProvider.toJson(body));
    }

    /**
     * GET /metrics - Returns application metrics.
     *
     * @return metrics response
     */
    public @NotNull ResponseEntity<?> metrics() {
        Map<String, Map<String, Object>> metrics = metricsRegistry.getMetrics();

        Map<String, Object> body = Map.of(
            "metrics", metrics,
            "count", metrics.size()
        );

        return ResponseEntity.ok(jsonProvider.toJson(body));
    }

    /**
     * GET /health/liveness - Kubernetes liveness probe.
     *
     * @return simple UP status
     */
    public @NotNull ResponseEntity<?> liveness() {
        // Liveness should always return 200 unless the process is truly dead
        // We just check the JVM is running
        Map<String, Object> body = Map.of(
            "status", "UP",
            "type", "liveness"
        );

        return ResponseEntity.ok(jsonProvider.toJson(body));
    }

    /**
     * GET /health/readiness - Kubernetes readiness probe.
     *
     * @return status based on health indicators
     */
    public @NotNull ResponseEntity<?> readiness() {
        // Readiness includes dependency checks
        Health health = healthIndicator.health();

        int statusCode = (health.status() == HealthStatus.UP || health.status() == HealthStatus.UNKNOWN)
            ? 200 : 503;

        Map<String, Object> body = Map.of(
            "status", health.status().code(),
            "type", "readiness"
        );

        return ResponseEntity.of(statusCode, jsonProvider.toJson(body));
    }

    /**
     * Returns the health indicator.
     *
     * @return the health indicator
     */
    public @NotNull CompositeHealthIndicator healthIndicator() {
        return healthIndicator;
    }

    /**
     * Returns the metrics registry.
     *
     * @return the metrics registry
     */
    public @NotNull MetricsRegistry metricsRegistry() {
        return metricsRegistry;
    }
}
