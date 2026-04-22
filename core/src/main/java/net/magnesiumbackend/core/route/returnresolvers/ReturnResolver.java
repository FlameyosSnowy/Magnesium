package net.magnesiumbackend.core.route.returnresolvers;

import net.magnesiumbackend.core.http.response.ResponseEntity;

import java.util.concurrent.CompletableFuture;

/**
 * Compile-time resolved adapter that transforms a controller return value
 * into a {@link CompletableFuture} of {@link ResponseEntity}.
 *
 * <p>Implementations are discovered at compile-time and wired into generated
 * route dispatchers. No runtime reflection is used.</p>
 *
 * @param <T> the return type this resolver handles
 */
public interface ReturnResolver<T> {

    /**
     * Resolves a return value to a CompletableFuture of ResponseEntity.
     *
     * @param value the controller method return value
     * @param ctx   the resolver context for recursive unwrapping
     * @return a CompletableFuture that will complete with the HTTP response
     */
    CompletableFuture<ResponseEntity<?>> resolve(T value, ResolverContext ctx);
}
