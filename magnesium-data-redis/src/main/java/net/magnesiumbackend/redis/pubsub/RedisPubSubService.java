package net.magnesiumbackend.redis.pubsub;

import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import net.magnesiumbackend.core.json.JsonProvider;
import net.magnesiumbackend.core.lifecycle.Startable;
import net.magnesiumbackend.core.lifecycle.Stoppable;
import net.magnesiumbackend.redis.RedisConfiguration;
import net.magnesiumbackend.redis.RedisService;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Pub/Sub service for Redis with reactive capabilities.
 *
 * <p>Supports both synchronous and asynchronous publishing, with
 * reactive subscription handling using virtual threads.</p>
 *
 * <p>Implements {@link Startable} and {@link Stoppable} for lifecycle
 * management in Magnesium's dependency graph.</p>
 */
public class RedisPubSubService implements Startable, Stoppable {

    private final RedisService redisService;
    private final RedisConfiguration configuration;
    private final JsonProvider jsonProvider;

    private StatefulRedisPubSubConnection<String, String> pubSubConnection;
    private final Map<String, Set<MessageHandler>> handlers = new ConcurrentHashMap<>();
    private final ExecutorService handlerExecutor;

    public RedisPubSubService(
        @NotNull RedisService redisService,
        @NotNull RedisConfiguration configuration,
        @NotNull JsonProvider jsonProvider
    ) {
        this.redisService = redisService;
        this.configuration = configuration;
        this.jsonProvider = jsonProvider;
        this.handlerExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public void start() throws Exception {
        if (!configuration.isPubSubEnabled()) {
            return;
        }

        // Create pub/sub connection
        this.pubSubConnection = ((io.lettuce.core.RedisClient) redisService.getClient())
            .connectPubSub();

        // Set up listener
        pubSubConnection.addListener(new RedisPubSubListener<>() {
            @Override
            public void message(String channel, String message) {
                dispatchMessage(channel, message, null);
            }

            @Override
            public void message(String pattern, String channel, String message) {
                dispatchMessage(channel, message, pattern);
            }

            @Override
            public void subscribed(String channel, long count) {
                // Log subscription
            }

            @Override
            public void psubscribed(String pattern, long count) {
                // Log pattern subscription
            }

            @Override
            public void unsubscribed(String channel, long count) {
                // Log unsubscription
            }

            @Override
            public void punsubscribed(String pattern, long count) {
                // Log pattern unsubscription
            }
        });
    }

    @Override
    public void stop() throws Exception {
        if (pubSubConnection != null) {
            pubSubConnection.close();
        }
        handlerExecutor.shutdown();
    }

    /**
     * Publishes a message to a channel.
     *
     * @param channel the channel name
     * @param message the message to publish
     */
    public void publish(@NotNull String channel, @NotNull String message) {
        pubSubConnection.sync().publish(channel, message);
    }

    /**
     * Publishes a message to a channel (async).
     *
     * @param channel the channel name
     * @param message the message to publish
     * @return future that completes when message is published
     */
    public CompletableFuture<Long> publishAsync(@NotNull String channel, @NotNull String message) {
        return pubSubConnection.async().publish(channel, message).toCompletableFuture();
    }

    /**
     * Publishes a typed message (serialized to JSON).
     *
     * @param channel the channel name
     * @param message the message object
     * @param <T> the message type
     */
    public <T> void publish(@NotNull String channel, @NotNull T message) {
        String json = jsonProvider.toJson(message);
        publish(channel, json);
    }

    /**
     * Publishes a typed message asynchronously.
     *
     * @param channel the channel name
     * @param message the message object
     * @param <T> the message type
     * @return future that completes when message is published
     */
    public <T> CompletableFuture<Long> publishAsync(@NotNull String channel, @NotNull T message) {
        String json = jsonProvider.toJson(message);
        return publishAsync(channel, json);
    }

    /**
     * Subscribes to a channel.
     *
     * @param channel the channel name
     * @param handler the message handler
     * @return subscription that can be unsubscribed
     */
    public @NotNull Subscription subscribe(@NotNull String channel, @NotNull MessageHandler handler) {
        handlers.computeIfAbsent(channel, k -> {
            pubSubConnection.sync().subscribe(channel);
            return new CopyOnWriteArraySet<>();
        }).add(handler);

        return () -> unsubscribe(channel, handler);
    }

    /**
     * Subscribes to a pattern (glob-style).
     *
     * @param pattern the pattern (e.g., "user.*")
     * @param handler the message handler
     * @return subscription that can be unsubscribed
     */
    public @NotNull Subscription psubscribe(@NotNull String pattern, @NotNull MessageHandler handler) {
        handlers.computeIfAbsent(pattern, k -> {
            pubSubConnection.sync().psubscribe(pattern);
            return new CopyOnWriteArraySet<>();
        }).add(handler);

        return () -> punsubscribe(pattern, handler);
    }

    private void unsubscribe(String channel, MessageHandler handler) {
        Set<MessageHandler> channelHandlers = handlers.get(channel);
        if (channelHandlers != null) {
            channelHandlers.remove(handler);
            if (channelHandlers.isEmpty()) {
                handlers.remove(channel);
                pubSubConnection.sync().unsubscribe(channel);
            }
        }
    }

    private void punsubscribe(String pattern, MessageHandler handler) {
        Set<MessageHandler> patternHandlers = handlers.get(pattern);
        if (patternHandlers != null) {
            patternHandlers.remove(handler);
            if (patternHandlers.isEmpty()) {
                handlers.remove(pattern);
                pubSubConnection.sync().punsubscribe(pattern);
            }
        }
    }

    private void dispatchMessage(String channel, String message, String pattern) {
        // Dispatch to exact channel handlers
        Set<MessageHandler> channelHandlers = handlers.get(channel);
        if (channelHandlers != null) {
            for (MessageHandler handler : channelHandlers) {
                handlerExecutor.submit(() -> handler.onMessage(channel, message, pattern));
            }
        }

        // Dispatch to pattern handlers
        if (pattern != null) {
            Set<MessageHandler> patternHandlers = handlers.get(pattern);
            if (patternHandlers != null) {
                for (MessageHandler handler : patternHandlers) {
                    handlerExecutor.submit(() -> handler.onMessage(channel, message, pattern));
                }
            }
        }
    }

    /**
     * Handler interface for pub/sub messages.
     */
    @FunctionalInterface
    public interface MessageHandler {
        /**
         * Called when a message is received.
         *
         * @param channel the channel the message was published to
         * @param message the message content
         * @param pattern the pattern that matched (null for exact channel subscriptions)
         */
        void onMessage(String channel, String message, String pattern);
    }

    /**
     * Represents a subscription that can be cancelled.
     */
    @FunctionalInterface
    public interface Subscription {
        /**
         * Unsubscribes from the channel/pattern.
         */
        void unsubscribe();
    }
}
