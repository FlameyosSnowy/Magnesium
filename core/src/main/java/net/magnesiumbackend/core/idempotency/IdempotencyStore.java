package net.magnesiumbackend.core.idempotency;

import net.magnesiumbackend.core.headers.HttpHeaderIndex;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

/**
 * Storage interface for idempotent response caching.
 *
 * <p>IdempotencyStore implementations cache HTTP responses keyed by idempotency
 * keys, allowing replay of identical responses for duplicate requests.
 * Implementations must handle TTL-based expiration.</p>
 *
 * <h3>Implementations</h3>
 * <ul>
 *   <li>{@link InMemoryIdempotencyStore} - In-memory with scheduled eviction</li>
 * </ul>
 *
 * <p>Custom implementations can use Redis, database, or other shared storage
 * for distributed deployments.</p>
 *
 * @see IdempotencyFilter
 * @see InMemoryIdempotencyStore
 */
public interface IdempotencyStore {

    /**
     * Stores a response for the given key.
     *
     * @param key      the idempotency key
     * @param response the response to cache
     * @param ttl      how long to retain the response
     */
    void store(String key, StoredResponse<?> response, Duration ttl);

    /**
     * Returns the stored response if present and not expired.
     *
     * @param key the idempotency key
     * @return the cached response, or empty if not found or expired
     */
    Optional<StoredResponse<?>> get(String key);

    /**
     * Record representing a cached HTTP response.
     *
     * @param <T>      the body type
     * @param statusCode the HTTP status code
     * @param body       the response body
     * @param headers    the response headers
     */
    record StoredResponse<T>(int statusCode, T body, HttpHeaderIndex headers) {}
}