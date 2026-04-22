package net.magnesiumbackend.amqp.health;

import net.magnesiumbackend.core.health.Health;
import net.magnesiumbackend.core.health.HealthIndicator;
import net.magnesiumbackend.amqp.RabbitMQService;
import org.jetbrains.annotations.NotNull;

/**
 * Health indicator for RabbitMQ.
 *
 * <p>Checks RabbitMQ connection status.</p>
 */
public final class RabbitMQHealthIndicator implements HealthIndicator {

    private final RabbitMQService rabbitMQService;

    public RabbitMQHealthIndicator(@NotNull RabbitMQService rabbitMQService) {
        this.rabbitMQService = rabbitMQService;
    }

    @Override
    public @NotNull Health health() {
        try {
            boolean connected = rabbitMQService.isConnected();

            if (!connected) {
                return Health.down()
                    .withDetail("status", "disconnected")
                    .build();
            }

            return Health.up()
                .withDetail("status", "connected")
                .build();

        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .withException(e)
                .build();
        }
    }

    @Override
    public @NotNull String name() {
        return "rabbitmq";
    }
}
