package net.magnesiumbackend.redis;

import net.magnesiumbackend.core.MagnesiumRuntime;
import net.magnesiumbackend.core.health.CompositeHealthIndicator;
import net.magnesiumbackend.core.health.HealthIndicatorContributor;
import net.magnesiumbackend.redis.health.RedisHealthIndicator;
import org.jetbrains.annotations.NotNull;

/**
 * Health indicator contributor for Redis.
 *
 * <p>Automatically registers the Redis health indicator when the
 * Redis module is present.</p>
 */
public class RedisHealthContributor implements HealthIndicatorContributor {

    @Override
    public void contribute(@NotNull CompositeHealthIndicator composite, @NotNull MagnesiumRuntime runtime) {
        try {
            var redisService = runtime.serviceRegistry().get(RedisService.class);
            composite.add(new RedisHealthIndicator(redisService));
        } catch (Exception e) {
            // Redis service not available (configuration missing or disabled)
        }
    }
}
