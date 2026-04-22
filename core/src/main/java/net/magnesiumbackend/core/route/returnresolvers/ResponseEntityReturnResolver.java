package net.magnesiumbackend.core.route.returnresolvers;

import net.magnesiumbackend.core.http.response.ResponseEntity;

import java.util.concurrent.CompletableFuture;

/**
 * Resolver for methods that already return a {@link ResponseEntity}.
 *
 * <p>This is a pass-through resolver — the response is already normalized
 * and can be returned directly. It serves as the transport-layer resolver
 * in the chain.</p>
 */
public final class ResponseEntityReturnResolver implements ReturnResolver<ResponseEntity<?>> {

    public static final ResponseEntityReturnResolver INSTANCE = new ResponseEntityReturnResolver();

    private ResponseEntityReturnResolver() {
    }

    @Override
    public CompletableFuture<ResponseEntity<?>> resolve(ResponseEntity<?> entity, ResolverContext ctx) {
        return CompletableFuture.completedFuture(entity);
    }
}
