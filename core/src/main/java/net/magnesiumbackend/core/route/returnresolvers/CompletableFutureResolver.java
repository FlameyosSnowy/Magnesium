package net.magnesiumbackend.core.route.returnresolvers;

import net.magnesiumbackend.core.http.response.ResponseEntity;

import java.util.concurrent.CompletableFuture;

/**
 * Resolves {@link CompletableFuture} return values by unwrapping the async
 * result and delegating the inner value to the next resolver in the chain.
 *
 * <p>Each resolver in Magnesium unwraps exactly one layer. This resolver
 * handles the async layer; the inner value is resolved via
 * {@link ResolverContext#resolveNext(Object)}.</p>
 */
public final class CompletableFutureResolver implements ReturnResolver<CompletableFuture<?>> {

    public static final CompletableFutureResolver INSTANCE = new CompletableFutureResolver();

    private CompletableFutureResolver() {
    }

    @Override
    public CompletableFuture<ResponseEntity<?>> resolve(CompletableFuture<?> future, ResolverContext ctx) {
        return future.thenCompose(ctx::resolveNext);
    }
}
