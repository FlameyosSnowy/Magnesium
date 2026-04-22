package net.magnesiumbackend.redis;

import io.lettuce.core.KeyValue;
import io.lettuce.core.ScoredValue;
import io.lettuce.core.Value;
import io.lettuce.core.api.sync.RedisCommands;
import net.magnesiumbackend.core.json.JsonProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * High-level template for Redis operations with JSON serialization.
 *
 * <p>Provides type-safe operations, automatic serialization/deserialization,
 * and convenient methods for common Redis use cases.</p>
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class RedisTemplate<K, V> {

    private final RedisService redisService;
    private final JsonProvider jsonProvider;
    private final Class<K> keyType;
    private final Class<V> valueType;
    private final String keyPrefix;

    /**
     * Creates a new RedisTemplate.
     *
     * @param redisService the Redis service
     * @param jsonProvider JSON provider for serialization
     * @param keyType key class
     * @param valueType value class
     * @param keyPrefix optional prefix for all keys
     */
    public RedisTemplate(
        @NotNull RedisService redisService,
        @NotNull JsonProvider jsonProvider,
        @NotNull Class<K> keyType,
        @NotNull Class<V> valueType,
        @Nullable String keyPrefix
    ) {
        this.redisService = redisService;
        this.jsonProvider = jsonProvider;
        this.keyType = keyType;
        this.valueType = valueType;
        this.keyPrefix = keyPrefix != null ? keyPrefix : "";
    }

    /**
     * Creates a template with default key prefix.
     */
    public RedisTemplate(
        @NotNull RedisService redisService,
        @NotNull JsonProvider jsonProvider,
        @NotNull Class<K> keyType,
        @NotNull Class<V> valueType
    ) {
        this(redisService, jsonProvider, keyType, valueType, null);
    }

    /**
     * Sets a value with optional TTL.
     */
    public void set(@NotNull K key, @NotNull V value, @Nullable Duration ttl) {
        String redisKey = serializeKey(key);
        String redisValue = jsonProvider.toJson(value);

        redisService.withSync(commands -> {
            if (ttl != null) {
                commands.setex(redisKey, ttl.getSeconds(), redisValue);
            } else {
                commands.set(redisKey, redisValue);
            }
            return null;
        });
    }

    /**
     * Sets a value without TTL.
     */
    public void set(@NotNull K key, @NotNull V value) {
        set(key, value, null);
    }

    /**
     * Gets a value by key.
     *
     * @return the value, or null if not found
     */
    public @Nullable V get(@NotNull K key) {
        String redisKey = serializeKey(key);
        String value = redisService.withSync(commands -> commands.get(redisKey));
        return value != null ? jsonProvider.fromJson(value, valueType) : null;
    }

    /**
     * Gets multiple values by keys.
     */
    public @NotNull Map<K, V> multiGet(@NotNull Set<K> keys) {
        List<String> redisKeys = new ArrayList<>(keys.size());
        for (K k : keys) {
            String s = serializeKey(k);
            redisKeys.add(s);
        }
        List<KeyValue<String, String>> results = redisService.withSync(
            commands -> commands.mget(redisKeys.toArray(new String[0]))
        );

        Map<K, V> map = new HashMap<>(results.size());
        for (KeyValue<String, String> kv : results) {
            if (kv.hasValue()) {
                K key = deserializeKey(kv.getKey());
                V value = jsonProvider.fromJson(kv.getValue(), valueType);
                map.put(key, value);
            }
        }
        return map;
    }

    /**
     * Deletes a key.
     *
     * @return true if key was deleted
     */
    public boolean delete(@NotNull K key) {
        String redisKey = serializeKey(key);
        Long deleted = redisService.withSync(commands -> commands.del(redisKey));
        return deleted != null && deleted > 0;
    }

    /**
     * Deletes multiple keys.
     *
     * @return number of deleted keys
     */
    public long delete(@NotNull Set<K> keys) {
        String[] redisKeys = new String[keys.size()];

        int i = 0;
        for (K key : keys) {
            redisKeys[i] = serializeKey(key);
            i++;
        }
        return redisService.withSync(commands -> commands.del(redisKeys));
    }

    /**
     * Checks if key exists.
     */
    public boolean hasKey(@NotNull K key) {
        String redisKey = serializeKey(key);
        return redisService.withSync(commands -> commands.exists(redisKey)) > 0;
    }

    /**
     * Sets TTL on a key.
     */
    public boolean expire(@NotNull K key, @NotNull Duration ttl) {
        String redisKey = serializeKey(key);
        return redisService.withSync(commands -> commands.expire(redisKey, ttl.getSeconds()));
    }

    /**
     * Gets remaining TTL of a key.
     */
    public long getExpire(@NotNull K key) {
        String redisKey = serializeKey(key);
        return redisService.withSync(commands -> commands.ttl(redisKey));
    }

    /**
     * Increments a numeric value.
     */
    public long increment(@NotNull K key) {
        String redisKey = serializeKey(key);
        return redisService.withSync(commands -> commands.incr(redisKey));
    }

    /**
     * Increments by a specific value.
     */
    public long increment(@NotNull K key, long delta) {
        String redisKey = serializeKey(key);
        return redisService.withSync(commands -> commands.incrby(redisKey, delta));
    }

    /**
     * Decrements a numeric value.
     */
    public long decrement(@NotNull K key) {
        String redisKey = serializeKey(key);
        return redisService.withSync(commands -> commands.decr(redisKey));
    }

    /**
     * Pushes value to left of list.
     */
    public long leftPush(@NotNull K key, @NotNull V value) {
        String redisKey = serializeKey(key);
        return redisService.withSync(commands -> commands.lpush(redisKey, jsonProvider.toJson(value)));
    }

    /**
     * Pushes value to right of list.
     */
    public long rightPush(@NotNull K key, @NotNull V value) {
        String redisKey = serializeKey(key);
        return redisService.withSync(commands -> commands.rpush(redisKey, jsonProvider.toJson(value)));
    }

    /**
     * Pops value from left of list.
     */
    public @Nullable V leftPop(@NotNull K key) {
        String redisKey = serializeKey(key);
        String value = redisService.withSync(commands -> commands.lpop(redisKey));
        return value != null ? jsonProvider.fromJson(value, valueType) : null;
    }

    /**
     * Pops value from right of list.
     */
    public @Nullable V rightPop(@NotNull K key) {
        String redisKey = serializeKey(key);
        String value = redisService.withSync(commands -> commands.rpop(redisKey));
        return value != null ? jsonProvider.fromJson(value, valueType) : null;
    }

    /**
     * Gets range of list elements.
     */
    public @NotNull List<V> range(@NotNull K key, long start, long end) {
        String redisKey = serializeKey(key);
        List<String> values = redisService.withSync(commands -> commands.lrange(redisKey, start, end));
        List<V> list = new ArrayList<>(values.size());
        for (String v : values) {
            V fromJson = jsonProvider.fromJson(v, valueType);
            list.add(fromJson);
        }
        return list;
    }

    /**
     * Adds member to set.
     */
    @SafeVarargs
    public final long addToSet(@NotNull K key, @NotNull V... members) {
        String redisKey = serializeKey(key);
        int length = members.length;
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            values[i] = jsonProvider.toJson(members[i]);
        }
        return redisService.withSync(commands -> commands.sadd(redisKey, values));
    }

    /**
     * Removes member from set.
     */
    @SafeVarargs
    public final long removeFromSet(@NotNull K key, @NotNull V... members) {
        String redisKey = serializeKey(key);

        int length = members.length;
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            values[i] = jsonProvider.toJson(members[i]);
        }
        return redisService.withSync(commands -> commands.srem(redisKey, values));
    }

    /**
     * Gets all members of a set.
     */
    public @NotNull Set<V> setMembers(@NotNull K key) {
        String redisKey = serializeKey(key);
        Set<String> members = redisService.withSync(commands -> commands.smembers(redisKey));
        Set<V> set = new HashSet<>(members.size());
        for (String v : members) {
            V fromJson = jsonProvider.fromJson(v, valueType);
            set.add(fromJson);
        }
        return set;
    }

    /**
     * Checks if member exists in set.
     */
    public boolean isMember(@NotNull K key, @NotNull V member) {
        String redisKey = serializeKey(key);
        return redisService.withSync(commands -> commands.sismember(redisKey, jsonProvider.toJson(member)));
    }

    /**
     * Adds member to sorted set with score.
     */
    public boolean addToZSet(@NotNull K key, @NotNull V member, double score) {
        String redisKey = serializeKey(key);
        Long result = redisService.withSync(commands -> commands.zadd(redisKey, score, jsonProvider.toJson(member)));
        return result != null && result > 0;
    }

    /**
     * Gets members of sorted set by score range.
     */
    public @NotNull List<V> zRangeByScore(@NotNull K key, double min, double max) {
        String redisKey = serializeKey(key);
        List<String> members = redisService.withSync(commands -> commands.zrangebyscore(redisKey, min, max));
        List<V> list = new ArrayList<>(members.size());
        for (String v : members) {
            V fromJson = jsonProvider.fromJson(v, valueType);
            list.add(fromJson);
        }
        return list;
    }

    /**
     * Gets members with scores from sorted set.
     */
    public List<Value<V>> zRangeWithScores(@NotNull K key, long start, long end) {
        String redisKey = serializeKey(key);
        List<ScoredValue<String>> scored = redisService.withSync(
            commands -> commands.zrangeWithScores(redisKey, start, end)
        );
        List<Value<V>> list = new ArrayList<>(scored.size());
        for (ScoredValue<String> sv : scored) {
            Value<V> vValue = ScoredValue.fromNullable(sv.getScore(), jsonProvider.fromJson(sv.getValue(), valueType));
            list.add(vValue);
        }
        return list;
    }

    /**
     * Removes member from sorted set.
     */
    @SafeVarargs
    public final long removeFromZSet(@NotNull K key, @NotNull V... members) {
        String redisKey = serializeKey(key);

        int length = members.length;
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            values[i] = jsonProvider.toJson(members[i]);
        }
        return redisService.withSync(commands -> commands.zrem(redisKey, values));
    }

    /**
     * Puts a hash entry.
     */
    public boolean putHash(@NotNull K key, @NotNull String hashKey, @NotNull V value) {
        String redisKey = serializeKey(key);
        return redisService.withSync(commands -> commands.hset(redisKey, hashKey, jsonProvider.toJson(value)));
    }

    /**
     * Gets a hash entry.
     */
    public @Nullable V getHash(@NotNull K key, @NotNull String hashKey) {
        String redisKey = serializeKey(key);
        String value = redisService.withSync(commands -> commands.hget(redisKey, hashKey));
        return value != null ? jsonProvider.fromJson(value, valueType) : null;
    }

    /**
     * Gets all hash entries.
     */
    public @NotNull Map<String, V> getHashAll(@NotNull K key) {
        String redisKey = serializeKey(key);
        Map<String, String> entries = redisService.withSync(commands -> commands.hgetall(redisKey));
        Map<String, V> result = new HashMap<>(entries.size());
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            result.put(entry.getKey(), jsonProvider.fromJson(entry.getValue(), valueType));
        }
        return result;
    }

    /**
     * Deletes hash entries.
     */
    public long deleteHash(@NotNull K key, @NotNull String... hashKeys) {
        String redisKey = serializeKey(key);
        return redisService.withSync(commands -> commands.hdel(redisKey, hashKeys));
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

    @SuppressWarnings("unchecked")
    private K deserializeKey(String keyStr) {
        String actualKey = keyStr.startsWith(keyPrefix) ? keyStr.substring(keyPrefix.length()) : keyStr;

        if (keyType == String.class) {
            return (K) actualKey;
        }
        return jsonProvider.fromJson(actualKey, keyType);
    }
}
