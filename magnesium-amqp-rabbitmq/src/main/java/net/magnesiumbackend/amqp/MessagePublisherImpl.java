package net.magnesiumbackend.amqp;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmCallback;
import net.magnesiumbackend.core.json.JsonProvider;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Internal implementation of {@link MessagePublisher} using Magnesium's JsonProvider.
 */
class MessagePublisherImpl<T> implements MessagePublisher<T> {

    private final RabbitMQService rabbitMQService;
    private final String exchange;
    private final String routingKey;
    private final Class<T> messageType;
    private final JsonProvider jsonProvider;
    private final Map<Long, CompletableFuture<Void>> pendingConfirms;

    MessagePublisherImpl(
        RabbitMQService rabbitMQService,
        String exchange,
        String routingKey,
        Class<T> messageType,
        JsonProvider jsonProvider
    ) {
        this.rabbitMQService = rabbitMQService;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.messageType = messageType;
        this.jsonProvider = jsonProvider;
        this.pendingConfirms = new ConcurrentHashMap<>();

        setupConfirmHandling();
    }

    private void setupConfirmHandling() {
        try {
            Channel channel = rabbitMQService.getChannel("publisher-" + exchange);
            channel.confirmSelect();

            channel.addConfirmListener(
                (seqNo, multiple) -> {
                    if (multiple) {
                        for (Long n : pendingConfirms.keySet()) {
                            if (n <= seqNo) {
                                completeConfirm(n, null);
                            }
                        }
                    } else {
                        completeConfirm(seqNo, null);
                    }
                },
                (seqNo, multiple) -> {
                    if (multiple) {
                        for (Long n : pendingConfirms.keySet()) {
                            if (n <= seqNo) {
                                completeConfirm(n, new RuntimeException("Message nacked"));
                            }
                        }
                    } else {
                        completeConfirm(seqNo, new RuntimeException("Message nacked"));
                    }
                }
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to setup publisher confirms", e);
        }
    }

    private void completeConfirm(long seqNo, Throwable error) {
        CompletableFuture<Void> future = pendingConfirms.remove(seqNo);
        if (future != null) {
            if (error != null) {
                future.completeExceptionally(error);
            } else {
                future.complete(null);
            }
        }
    }

    @Override
    public void publish(@NotNull T message) {
        publish(message, routingKey);
    }

    @Override
    public void publish(@NotNull T message, @NotNull String routingKey) {
        publish(message, routingKey, Map.of());
    }

    @Override
    public void publish(@NotNull T message, @NotNull String routingKey, @NotNull Map<String, Object> headers) {
        try {
            Channel channel = rabbitMQService.getChannel("publisher-" + exchange);
            byte[] body = jsonProvider.toJsonBytes(message);

            AMQP.BasicProperties.Builder propsBuilder = new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .headers(headers);

            channel.basicPublish(exchange, routingKey, propsBuilder.build(), body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish message", e);
        }
    }

    @Override
    public CompletableFuture<Void> publishAsync(@NotNull T message) {
        return publishAsync(message, routingKey, Map.of());
    }

    public CompletableFuture<Void> publishAsync(@NotNull T message, @NotNull String routingKey, @NotNull Map<String, Object> headers) {
        try {
            Channel channel = rabbitMQService.getChannel("publisher-" + exchange);
            byte[] body = jsonProvider.toJsonBytes(message);

            AMQP.BasicProperties.Builder propsBuilder = new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .headers(headers);

            long seqNo = channel.getNextPublishSeqNo();
            CompletableFuture<Void> future = new CompletableFuture<>();
            pendingConfirms.put(seqNo, future);

            channel.basicPublish(exchange, routingKey, propsBuilder.build(), body);

            return future;
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public void publishWithTtl(@NotNull T message, @NotNull Duration ttl) {
        try {
            Channel channel = rabbitMQService.getChannel("publisher-" + exchange);
            byte[] body = jsonProvider.toJsonBytes(message);

            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .expiration(String.valueOf(ttl.toMillis()))
                .build();

            channel.basicPublish(exchange, routingKey, props, body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish message with TTL", e);
        }
    }

    @Override
    public void publishWithPriority(@NotNull T message, int priority) {
        try {
            Channel channel = rabbitMQService.getChannel("publisher-" + exchange);
            byte[] body = jsonProvider.toJsonBytes(message);

            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .priority(priority)
                .build();

            channel.basicPublish(exchange, routingKey, props, body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish message with priority", e);
        }
    }

    @Override
    public void publishDelayed(@NotNull T message, @NotNull Duration delay) {
        // Requires rabbitmq-delayed-message-exchange plugin
        Map<String, Object> headers = Map.of("x-delay", delay.toMillis());
        publish(message, routingKey, headers);
    }
}
