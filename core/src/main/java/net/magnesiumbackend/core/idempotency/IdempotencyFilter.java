package net.magnesiumbackend.core.idempotency;

import net.magnesiumbackend.core.headers.Slice;
import net.magnesiumbackend.core.http.ErrorResponse;
import net.magnesiumbackend.core.http.HttpMethod;
import net.magnesiumbackend.core.http.ResponseEntity;
import net.magnesiumbackend.core.route.FilterChain;
import net.magnesiumbackend.core.route.HttpFilter;
import net.magnesiumbackend.core.route.RequestContext;

import java.time.Duration;
import java.util.Optional;

public final class IdempotencyFilter implements HttpFilter {
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    private static final String REPLAYED_HEADER    = "Idempotent-Replayed";

    private final IdempotencyStore store;
    private final Duration ttlHours;

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

        Slice key = ctx.header(IDEMPOTENCY_HEADER);
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