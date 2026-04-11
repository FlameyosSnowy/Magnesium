package net.magnesiumbackend.core.idempotency;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class InMemoryIdempotencyStore implements IdempotencyStore {

    private static final java.util.concurrent.ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "magnesium-idempotency-eviction");
        t.setDaemon(true);
        return t;
    });

    private record Entry(StoredResponse<?> response, Instant expiresAt) {}

    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();

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