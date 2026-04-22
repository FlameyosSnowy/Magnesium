package net.magnesiumbackend.amqp;

import net.magnesiumbackend.core.MagnesiumRuntime;
import net.magnesiumbackend.core.health.CompositeHealthIndicator;
import net.magnesiumbackend.core.health.HealthIndicatorContributor;
import net.magnesiumbackend.amqp.health.RabbitMQHealthIndicator;
import org.jetbrains.annotations.NotNull;

/**
 * Health indicator contributor for RabbitMQ.
 *
 * <p>Automatically registers the RabbitMQ health indicator when the
 * RabbitMQ module is present.</p>
 */
public class RabbitMQHealthContributor implements HealthIndicatorContributor {

    @Override
    public void contribute(@NotNull CompositeHealthIndicator composite, @NotNull MagnesiumRuntime runtime) {
        try {
            var rabbitService = runtime.serviceRegistry().get(RabbitMQService.class);
            composite.add(new RabbitMQHealthIndicator(rabbitService));
        } catch (Exception e) {
            // RabbitMQ service not available (configuration missing or disabled)
        }
    }
}
