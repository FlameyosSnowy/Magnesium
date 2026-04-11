package net.magnesiumbackend.core.route;

import net.magnesiumbackend.core.http.response.ResponseEntity;

import java.util.List;

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
            return globalFilters.get(globalIndex++).handle(ctx, this);
        }
        if (routeIndex < routeFilters.size()) {
            return routeFilters.get(routeIndex++).handle(ctx, this);
        }
        return handler.handle(ctx);
    }
}