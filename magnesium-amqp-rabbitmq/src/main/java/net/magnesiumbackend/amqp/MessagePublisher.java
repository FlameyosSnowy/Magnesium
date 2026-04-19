package net.magnesiumbackend.amqp;

import com.rabbitmq.client.AMQP;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * High-level message publisher for RabbitMQ.
 *
 * <p>Provides typed message publishing with routing, headers, and
 * confirmation support.</p>
 *
 * @param <T> the message type
 */
public interface MessagePublisher<T> {

    /**
     * Publishes a message to the configured exchange.
     *
     * @param message the message to publish
     */
    void publish(@NotNull T message);

    /**
     * Publishes a message with a specific routing key.
     *
     * @param message the message to publish
     * @param routingKey the routing key
     */
    void publish(@NotNull T message, @NotNull String routingKey);

    /**
     * Publishes a message with custom properties.
     *
     * @param message the message to publish
     * @param routingKey the routing key
     * @param headers message headers
     */
    void publish(@NotNull T message, @NotNull String routingKey, @NotNull Map<String, Object> headers);

    /**
     * Publishes a message asynchronously and returns a confirmation future.
     *
     * @param message the message to publish
     * @return future that completes when broker confirms receipt
     */
    CompletableFuture<Void> publishAsync(@NotNull T message);

    /**
     * Publishes a message with a custom TTL.
     *
     * @param message the message to publish
     * @param ttl message time-to-live
     */
    void publishWithTtl(@NotNull T message, @NotNull java.time.Duration ttl);

    /**
     * Publishes a message with priority.
     *
     * @param message the message to publish
     * @param priority message priority (0-9)
     */
    void publishWithPriority(@NotNull T message, int priority);

    /**
     * Publishes a message with delay (requires delayed message plugin).
     *
     * @param message the message to publish
     * @param delay the delay before delivery
     */
    void publishDelayed(@NotNull T message, @NotNull java.time.Duration delay);
}
