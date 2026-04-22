package net.magnesium.universal;

import io.github.flameyossnowy.universal.api.cache.TransactionResult;
import io.github.flameyossnowy.universal.api.exceptions.RepositoryException;
import io.github.flameyossnowy.universal.api.exceptions.UpdateRepositoryException;
import net.magnesiumbackend.core.annotations.ReturnResolverType;
import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.route.returnresolvers.ResolverContext;
import net.magnesiumbackend.core.route.returnresolvers.ReturnResolver;

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
@ReturnResolverType(TransactionResult.class)
public final class DefaultTransactionResultResolver implements ReturnResolver<TransactionResult<?>> {

    private DefaultTransactionResultResolver() {
    }

    @Override
    public CompletableFuture<ResponseEntity<?>> resolve(TransactionResult<?> value, ResolverContext ctx) {
        if (value == null) {
            return CompletableFuture.completedFuture(ResponseEntity.noContent());
        }
        if (value.isSuccess()) {
            try {
                return CompletableFuture.completedFuture(ResponseEntity.ok(value.get()));
            } catch (Throwable e) {
                // This should never happen.
                return CompletableFuture.completedFuture(ResponseEntity.of(500, e.getMessage()));
            }
        }

        // Error is guaranteed
        Throwable error = value.error();
        if (error instanceof RepositoryException e) {
            throw e;
        }

        return CompletableFuture.completedFuture(ResponseEntity.of(500, error.getMessage()));
    }
}
