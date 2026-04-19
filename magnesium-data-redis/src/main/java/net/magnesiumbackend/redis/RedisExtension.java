package net.magnesiumbackend.redis;

import net.magnesiumbackend.core.MagnesiumRuntime;
import net.magnesiumbackend.core.extensions.MagnesiumExtension;
import net.magnesiumbackend.core.json.JsonProvider;
import net.magnesiumbackend.redis.pubsub.RedisPubSubService;
import org.jetbrains.annotations.NotNull;

/**
 * Redis extension for Magnesium framework.
 *
 * <p>Integrates Redis with Lettuce driver, providing:</p>
 * <ul>
 *   <li>Connection management (single node, cluster, sentinel)</li>
 *   <li>High-level {@link RedisTemplate}</li>
 *   <li>Pub/Sub support</li>
 *   <li>Reactive API</li>
 *   <li>Pipelining</li>
 * </ul>
 */
public class RedisExtension implements MagnesiumExtension {

    @Override
    public void configure(@NotNull MagnesiumRuntime runtime) {
        runtime.services(services -> {
            // Register configuration
            services.register(RedisConfiguration.class, ctx ->
                ctx.configurationManager().get(RedisConfiguration.class)
            );

            // Register RedisService (Startable/Stoppable)
            services.register(RedisService.class, ctx -> {
                RedisConfiguration config = ctx.get(RedisConfiguration.class);
                return new RedisService(config);
            });

            // Register Pub/Sub service if enabled
            services.register(RedisPubSubService.class, ctx -> {
                RedisConfiguration config = ctx.get(RedisConfiguration.class);
                if (!config.isPubSubEnabled()) {
                    return null;
                }
                return new RedisPubSubService(
                    ctx.get(RedisService.class),
                    config,
                    ctx.configurationManager().get(JsonProvider.class)
                );
            });
        });
    }

    /**
     * Creates a RedisTemplate factory method.
     *
     * @param <K> key type
     * @param <V> value type
     * @param keyType key class
     * @param valueType value class
     * @param keyPrefix optional key prefix
     * @return template instance
     */
    public static <K, V> RedisTemplate<K, V> createTemplate(
        RedisService redisService,
        JsonProvider jsonProvider,
        Class<K> keyType,
        Class<V> valueType,
        String keyPrefix
    ) {
        return new RedisTemplate<>(redisService, jsonProvider, keyType, valueType, keyPrefix);
    }

    /**
     * Creates a reactive operations instance.
     *
     * @param <K> key type
     * @param <V> value type
     * @param keyType key class
     * @param valueType value class
     * @param keyPrefix optional key prefix
     * @return reactive operations instance
     */
    public static <K, V> net.magnesiumbackend.redis.reactive.ReactiveRedisOperations<K, V> createReactive(
        RedisService redisService,
        JsonProvider jsonProvider,
        Class<K> keyType,
        Class<V> valueType,
        String keyPrefix
    ) {
        return new net.magnesiumbackend.redis.reactive.ReactiveRedisOperations<>(
            redisService, jsonProvider, keyType, valueType, keyPrefix);
    }

    @Override
    public @NotNull String name() {
        return "redis";
    }
}
