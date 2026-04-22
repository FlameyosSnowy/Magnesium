package net.magnesiumbackend.core.health;

import net.magnesiumbackend.core.MagnesiumRuntime;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for extensions that contribute health indicators to the actuator.
 *
 * <p>Implementations are discovered via {@link java.util.ServiceLoader} and called
 * during actuator initialization to register their health indicators.</p>
 *
 * <h3>Implementation</h3>
 * <pre>{@code
 * public class RedisHealthContributor implements HealthIndicatorContributor {
 *     @Override
 *     public void contribute(@NotNull CompositeHealthIndicator composite, @NotNull MagnesiumRuntime runtime) {
 *         var redisService = runtime.serviceRegistry().get(RedisService.class);
 *         composite.add(new RedisHealthIndicator(redisService));
 *     }
 * }
 * }</pre>
 *
 * <h3>Registration</h3>
 * Add to your module-info.java:
 * <pre>{@code
 * provides net.magnesiumbackend.core.health.HealthIndicatorContributor
 *     with com.example.YourHealthContributor;
 * }</pre>
 *
 * @see CompositeHealthIndicator
 * @see HealthIndicator
 */
@FunctionalInterface
public interface HealthIndicatorContributor {

    /**
     * Contributes health indicators to the composite health check.
     *
     * @param composite the composite health indicator to add to
     * @param runtime the Magnesium runtime for accessing services
     */
    void contribute(@NotNull CompositeHealthIndicator composite, @NotNull MagnesiumRuntime runtime);
}
