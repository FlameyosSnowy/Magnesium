package net.magnesiumbackend.core.idempotency;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * In-memory implementation of {@link IdempotencyStore} with TTL-based eviction.
 *
 * <p>Stores idempotent responses in a concurrent hash map with scheduled
 * background eviction of expired entries. Uses a daemon thread for cleanup.</p>
 *
 * <h3>Limitations</h3>
 * <ul>
 *   <li>Memory-only: responses lost on restart</li>
 *   <li>Single-node: not suitable for distributed deployments</li>
 *   <li>Use Redis or database for production clustering</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * IdempotencyFilter filter = new IdempotencyFilter(
 *     new InMemoryIdempotencyStore(),
 *     24 // TTL hours
 * );
 * }</pre>
 *
 * @see IdempotencyStore
 * @see IdempotencyFilter
 */
public final class InMemoryIdempotencyStore implements IdempotencyStore {

    /** Background executor for entry eviction. */
    private static final java.util.concurrent.ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "magnesium-idempotency-eviction");
        t.setDaemon(true);
        return t;
    });

    /** Internal entry with expiration timestamp. */
    private record Entry(StoredResponse<?> response, Instant expiresAt) {}

    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>(32);

    /**
     * Creates a new in-memory idempotency store with scheduled eviction.
     */
    public InMemoryIdempotencyStore() {
        SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(this::evict, 5, 5, TimeUnit.MINUTES);
    }

    @Override
    public void store(String key, StoredResponse<?> response, Duration ttl) {
        store.put(key, new Entry(response, Instant.now().plus(ttl)));
    }

    @Override
    public Optional<StoredResponse<?>> get(String key) {
        Entry entry = store.get(key);
        if (entry == null) return Optional.empty();
        if (Instant.now().isAfter(entry.expiresAt())) {
            store.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.response());
    }

    private void evict() {
        Instant now = Instant.now();
        store.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt()));
    }
}