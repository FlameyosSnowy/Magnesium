package net.magnesiumbackend.core.health;

import org.jetbrains.annotations.NotNull;

/**
 * Interface for health checks.
 *
 * <p>Implementations provide health status for specific components
 * (databases, message queues, external services, etc.).</p>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * public class DatabaseHealthIndicator implements HealthIndicator {
 *     @Override
 *     public Health health() {
 *         try {
 *             connection.verify();
 *             return Health.up().withDetail("connections", 5).build();
 *         } catch (Exception e) {
 *             return Health.down().withException(e).build();
 *         }
 *     }
 *
 *     @Override
 *     public String name() {
 *         return "database";
 *     }
 * }
 * }</pre>
 *
 * @see Health
 * @see CompositeHealthIndicator
 */
@FunctionalInterface
public interface HealthIndicator {

    /**
     * Performs the health check.
     *
     * @return the current health status
     */
    @NotNull Health health();

    /**
     * Returns the name of this indicator (used in health endpoint).
     *
     * @return indicator name
     */
    default @NotNull String name() {
        return getClass().getSimpleName()
            .replace("HealthIndicator", "")
            .toLowerCase();
    }
}
