package net.magnesiumbackend.core.route;

import net.magnesiumbackend.core.http.response.ResponseEntity;

import java.util.List;

/**
 * Default implementation of {@link FilterChain} that supports both sync and async filters.
 *
 * <p>Uses {@link ResponseEntityResolver} to handle filters returning either ResponseEntity,
 * CompletableFuture, or raw values.</p>
 */
public final class DefaultFilterChain implements FilterChain {
    private final List<HttpFilter> globalFilters;
    private final List<HttpFilter> routeFilters;
    private final HttpRouteHandler handler;

    private int globalIndex = 0;
    private int routeIndex = 0;

    public DefaultFilterChain(
        List<HttpFilter> globalFilters,
        List<HttpFilter> routeFilters,
        HttpRouteHandler handler
    ) {
        this.globalFilters = globalFilters;
        this.routeFilters = routeFilters;
        this.handler = handler;
    }

    @Override
    public ResponseEntity<?> next(RequestContext ctx) {
        if (globalIndex < globalFilters.size()) {
            Object result = globalFilters.get(globalIndex++).handle(ctx, this);
            return ResponseEntityResolver.resolveSync(result);
        }
        if (routeIndex < routeFilters.size()) {
            Object result = routeFilters.get(routeIndex++).handle(ctx, this);
            return ResponseEntityResolver.resolveSync(result);
        }
        Object result = handler.handle(ctx);
        return ResponseEntityResolver.resolveSync(result);
    }
}