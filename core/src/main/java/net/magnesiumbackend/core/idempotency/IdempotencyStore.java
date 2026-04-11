package net.magnesiumbackend.core.idempotency;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public interface IdempotencyStore {
    /**
     * Store a response for a key. TTL is how long to retain it.
     */
    void store(String key, StoredResponse<?> response, Duration ttl);

    /**
     * Returns the stored response if present and not expired, else empty.
     */
    Optional<StoredResponse<?>> get(String key);

    record StoredResponse<T>(int statusCode, T body, Map<String, String> headers) {}
}