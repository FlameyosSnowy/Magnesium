package net.magnesiumbackend.core.idempotency;

import net.magnesiumbackend.core.headers.Slice;
import net.magnesiumbackend.core.http.response.ErrorResponse;
import net.magnesiumbackend.core.http.response.HttpMethod;
import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.route.FilterChain;
import net.magnesiumbackend.core.route.HttpFilter;
import net.magnesiumbackend.core.route.RequestContext;

import java.time.Duration;
import java.util.Optional;

/**
 * HTTP filter that provides idempotency for mutating requests.
 *
 * <p>IdempotencyFilter ensures that requests with the same Idempotency-Key
 * header return the same response, preventing duplicate operations. It:
 * <ul>
 *   <li>Requires Idempotency-Key header for POST/PUT/DELETE/PATCH requests</li>
 *   <li>Stores successful responses keyed by (path + idempotency key)</li>
 *   <li>Returns cached response with Idempotent-Replayed header for replays</li>
 *   <li>Automatically expires stored responses after TTL</li>
 * </ul>
 * </p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * MagnesiumApplication.builder()
 *     .filter(new IdempotencyFilter(new InMemoryIdempotencyStore(), 24))
 *     .build();
 * }
 *
 * // Client sends:
 * POST /payments
 * Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
 *
 * // Replay returns same response with header:
 * Idempotent-Replayed: true
 * </pre>
 *
 * @see IdempotencyStore
 * @see InMemoryIdempotencyStore
 * @see HttpFilter
 */
public final class IdempotencyFilter implements HttpFilter {
    /** Header for the idempotency key. */
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    /** Header added to replayed responses. */
    private static final String REPLAYED_HEADER    = "Idempotent-Replayed";

    private final IdempotencyStore store;
    private final Duration ttlHours;

    /**
     * Creates a new idempotency filter.
     *
     * @param store   the store for caching responses
     * @param ttlHours hours to retain stored responses
     */
    public IdempotencyFilter(IdempotencyStore store, long ttlHours) {
        this.store    = store;
        this.ttlHours = Duration.ofHours(ttlHours);
    }

    @Override
    public ResponseEntity<?> handle(RequestContext ctx, FilterChain chain) {
        // Only meaningful for mutating methods
        HttpMethod method = ctx.request().method();
        if (method == HttpMethod.GET || method == HttpMethod.HEAD) {
            return chain.next(ctx);
        }

        Slice key = ctx.headerRaw(IDEMPOTENCY_HEADER);
        if (key == null || key.isBlank()) {
            return ResponseEntity.of(400, new ErrorResponse(
                "missing_idempotency_key",
                "This endpoint requires an Idempotency-Key header."
            ));
        }

        // Namespaced by path to prevent cross-endpoint key collisions
        String storeKey = ctx.request().path() + ":" + key.materialize();

        Optional<IdempotencyStore.StoredResponse<?>> cached = store.get(storeKey);
        if (cached.isPresent()) {
            IdempotencyStore.StoredResponse<?> stored = cached.get();
            ResponseEntity<?> replayed = ResponseEntity.of(stored.statusCode(), stored.body());
            replayed.headers().put(REPLAYED_HEADER, "true");
            return replayed;
        }

        ResponseEntity<?> response = chain.next(ctx);

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            store.store(storeKey, new IdempotencyStore.StoredResponse<>(
                response.statusCode(),
                response.body(),
                response.headers()
            ), ttlHours);
        }

        return response;
    }
}