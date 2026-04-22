package net.magnesiumbackend.core.route.returnresolvers;

import net.magnesiumbackend.core.http.response.ResponseEntity;

import java.util.concurrent.CompletableFuture;

/**
 * Default resolver for raw return values that are not wrapped in any
 * recognized domain or async type.
 *
 * <p>Wraps the value in a {@code 200 OK} {@link ResponseEntity}. This is the
 * terminal resolver in the resolution chain.</p>
 *
 * <p>Null values produce a 204 No Content response.</p>
 */
public final class DefaultObjectResolver implements ReturnResolver<Object> {

    public static final DefaultObjectResolver INSTANCE = new DefaultObjectResolver();

    private DefaultObjectResolver() {
    }

    @Override
    public CompletableFuture<ResponseEntity<?>> resolve(Object value, ResolverContext ctx) {
        if (value == null) {
            return CompletableFuture.completedFuture(ResponseEntity.noContent());
        }
        return CompletableFuture.completedFuture(ResponseEntity.ok(value));
    }
}
