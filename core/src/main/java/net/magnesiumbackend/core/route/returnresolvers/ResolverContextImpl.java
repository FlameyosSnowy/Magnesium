package net.magnesiumbackend.core.route.returnresolvers;

import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.route.RequestContext;

import java.util.concurrent.CompletableFuture;

/**
 * Default implementation of {@link ResolverContext}.
 *
 * <p>Provides recursive resolution using the framework's built-in resolver
 * chain. This is the context passed to custom resolvers at runtime.</p>
 */
public final class ResolverContextImpl implements ResolverContext {

    private final RequestContext request;

    public ResolverContextImpl(RequestContext request) {
        this.request = request;
    }

    @Override
    public CompletableFuture<ResponseEntity<?>> resolveNext(Object value) {
        return switch (value) {
            case null -> DefaultObjectResolver.INSTANCE.resolve(null, this);
            case CompletableFuture<?> future -> CompletableFutureResolver.INSTANCE.resolve(future, this);
            case ResponseEntity<?> entity -> ResponseEntityReturnResolver.INSTANCE.resolve(entity, this);
            default -> DefaultObjectResolver.INSTANCE.resolve(value, this);
        };

    }

    @Override
    public RequestContext requestContext() {
        return request;
    }
}
