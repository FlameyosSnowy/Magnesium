package net.magnesiumbackend.core.route.returnresolvers;

import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.route.RequestContext;

import java.util.concurrent.CompletableFuture;

/**
 * Context passed to {@link ReturnResolver} implementations for recursive
 * resolution and pipeline control.
 */
public interface ResolverContext {

    /**
     * Recursively resolves the next layer of a wrapped return value.
     *
     * <p>Each resolver unwraps exactly one layer and delegates the inner
     * value back to the framework for further resolution.</p>
     *
     * @param value the inner value to resolve
     * @return a CompletableFuture of the resolved HTTP response
     */
    CompletableFuture<ResponseEntity<?>> resolveNext(Object value);

    /**
     * The current HTTP request context.
     */
    RequestContext requestContext();
}
