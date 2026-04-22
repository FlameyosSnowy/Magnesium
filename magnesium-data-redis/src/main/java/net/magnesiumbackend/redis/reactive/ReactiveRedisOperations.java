package net.magnesiumbackend.redis.reactive;

import io.lettuce.core.Range;
import io.lettuce.core.ScoredValue;
import net.magnesiumbackend.core.json.JsonProvider;
import net.magnesiumbackend.redis.RedisService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Reactive Redis operations using Project Reactor.
 *
 * <p>Provides non-blocking, reactive API for all Redis data types.
 * Uses Lettuce's native reactive support.</p>
 *
 * @param <K> key type
 * @param <V> value type
 */
@SuppressWarnings("unused")
public class ReactiveRedisOperations<K, V> {

    private final RedisService redisService;
    private final JsonProvider jsonProvider;
    private final Class<V> valueType;
    private final String keyPrefix;

    public ReactiveRedisOperations(
        @NotNull RedisService redisService,
        @NotNull JsonProvider jsonProvider,
        @NotNull Class<V> valueType,
        @Nullable String keyPrefix
    ) {
        this.redisService = redisService;
        this.jsonProvider = jsonProvider;
        this.valueType = valueType;
        this.keyPrefix = keyPrefix != null ? keyPrefix : "";
    }

    /**
     * Sets a value with optional TTL.
     */
    public Mono<String> set(@NotNull K key, @NotNull V value, @Nullable Duration ttl) {
        String redisKey = serializeKey(key);
        String redisValue = jsonProvider.toJson(value);

        if (ttl != null) {
            return Mono.fromCompletionStage(redisService.withAsync(async -> async.setex(redisKey, ttl.getSeconds(), redisValue)));
        }
        return Mono.fromCompletionStage(redisService.withAsync(async -> async.set(redisKey, redisValue)));
    }

    /**
     * Gets a value by key.
     */
    public Mono<V> get(@NotNull K key) {
        String redisKey = serializeKey(key);
        return Mono.fromCompletionStage(redisService.withAsync(async -> async.get(redisKey)))
            .map(v -> v != null ? jsonProvider.fromJson(v, valueType) : null);
    }

    public Mono<Long> delete(@NotNull K key) {
        String redisKey = serializeKey(key);
        return Mono.fromCompletionStage(
            redisService.withAsync(async -> async.del(redisKey))
        );
    }

    public Mono<Long> decrement(@NotNull K key) {
        String redisKey = serializeKey(key);
        return Mono.fromCompletionStage(
            redisService.withAsync(async -> async.decr(redisKey))
        );
    }

    public Mono<Long> leftPush(@NotNull K key, @NotNull V value) {
        String redisKey = serializeKey(key);
        return Mono.fromCompletionStage(
            redisService.withAsync(async -> async.lpush(redisKey, jsonProvider.toJson(value)))
        );
    }

    public Mono<Long> rightPush(@NotNull K key, @NotNull V value) {
        String redisKey = serializeKey(key);
        return Mono.fromCompletionStage(
            redisService.withAsync(async -> async.rpush(redisKey, jsonProvider.toJson(value)))
        );
    }

    @SafeVarargs
    public final Mono<Long> addToSet(@NotNull K key, @NotNull V... members) {
        String redisKey = serializeKey(key);

        int length = members.length;
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            values[i] = jsonProvider.toJson(members[i]);
        }

        return Mono.fromCompletionStage(
            redisService.withAsync(async -> async.sadd(redisKey, values))
        );
    }

    public Mono<Long> addToZSet(@NotNull K key, @NotNull V member, double score) {
        String redisKey = serializeKey(key);
        return Mono.fromCompletionStage(
            redisService.withAsync(async -> async.zadd(redisKey, score, jsonProvider.toJson(member)))
        );
    }

    public Flux<ScoredValue<V>> zRangeWithScores(@NotNull K key, long start, long end) {
        String redisKey = serializeKey(key);

        return Mono.fromCompletionStage(
                redisService.withAsync(async -> async.zrangeWithScores(redisKey, start, end))
            )
            .flatMapIterable(list -> list)
            .map(sv -> (ScoredValue<V>) ScoredValue.fromNullable(
                sv.getScore(),
                jsonProvider.fromJson(sv.getValue(), valueType)
            ));
    }

    /**
     * Checks if key exists.
     */
    public Mono<Boolean> hasKey(@NotNull K key) {
        String redisKey = serializeKey(key);
        return Mono.fromCompletionStage(redisService.withAsync(async -> async.exists(redisKey)))
            .map(count -> count > 0);
    }

    /**
     * Sets TTL on a key.
     */
    public Mono<Boolean> expire(@NotNull K key, @NotNull Duration ttl) {
        String redisKey = serializeKey(key);
        return Mono.fromCompletionStage(redisService.withAsync(async -> async.expire(redisKey, ttl.getSeconds())));
    }

    public Mono<Long> increment(@NotNull K key) {
        String redisKey = serializeKey(key);
        return Mono.fromCompletionStage(
            redisService.withAsync(async -> async.incr(redisKey))
        );
    }

    public Mono<Long> increment(@NotNull K key, long delta) {
        String redisKey = serializeKey(key);
        return Mono.fromCompletionStage(
            redisService.withAsync(async -> async.incrby(redisKey, delta))
        );
    }

    public Mono<V> rightPop(@NotNull K key) {
        String redisKey = serializeKey(key);
        return Mono.fromCompletionStage(redisService.withAsync(async -> async.rpop(redisKey)))
            .map(v -> v != null ? jsonProvider.fromJson(v, valueType) : null);
    }

    public Flux<V> range(@NotNull K key, long start, long end) {
        String redisKey = serializeKey(key);
        return Mono.fromCompletionStage(redisService.withAsync(async -> async.lrange(redisKey, start, end)))
            .flatMapIterable(list -> list)
            .map(v -> jsonProvider.fromJson(v, valueType));
    }

    public Flux<V> setMembers(@NotNull K key) {
        String redisKey = serializeKey(key);
        return Mono.fromCompletionStage(redisService.withAsync(async -> async.smembers(redisKey)))
            .flatMapIterable(set -> set)
            .map(v -> jsonProvider.fromJson(v, valueType));
    }

    public Mono<Boolean> isMember(@NotNull K key, @NotNull V member) {
        String redisKey = serializeKey(key);
        return Mono.fromCompletionStage(redisService.withAsync(async -> async.sismember(redisKey, jsonProvider.toJson(member))));
    }

    public Flux<V> zRangeByScore(@NotNull K key, double min, double max) {
        String redisKey = serializeKey(key);
        return Mono.fromCompletionStage(redisService.withAsync(async -> async.zrangebyscore(redisKey, Range.create(min, max))))
            .flatMapIterable(list -> list)
            .map(v -> jsonProvider.fromJson(v, valueType));
    }

    public Mono<Boolean> putHash(@NotNull K key, @NotNull String hashKey, @NotNull V value) {
        String redisKey = serializeKey(key);
        return Mono.fromCompletionStage(redisService.withAsync(async -> async.hset(redisKey, hashKey, jsonProvider.toJson(value))));
    }

    public Mono<V> getHash(@NotNull K key, @NotNull String hashKey) {
        String redisKey = serializeKey(key);
        return Mono.fromCompletionStage(redisService.withAsync(async -> async.hget(redisKey, hashKey)))
            .map(v -> v != null ? jsonProvider.fromJson(v, valueType) : null);
    }

    public Mono<Map<String, V>> getHashAll(@NotNull K key) {
        String redisKey = serializeKey(key);
        return Mono.fromCompletionStage(redisService.withAsync(async -> async.hgetall(redisKey)))
            .handle((entries, sink) -> {
                Map<String, V> map = new HashMap<>(entries.size());
                for (Map.Entry<String, String> e : entries.entrySet()) {
                    if (map.put(e.getKey(), jsonProvider.fromJson(e.getValue(), valueType)) != null) {
                        sink.error(new IllegalStateException("Duplicate key"));
                        return;
                    }
                }
                sink.next(map);
            });
    }

    private String serializeKey(K key) {
        String keyStr;
        if (key instanceof String s) {
            keyStr = s;
        } else {
            keyStr = jsonProvider.toJson(key);
        }
        return keyPrefix + keyStr;
    }
}
