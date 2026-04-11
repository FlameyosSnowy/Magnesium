package net.magnesiumbackend.core.route;

import net.magnesiumbackend.core.http.HttpMethod;
import net.magnesiumbackend.core.http.ResponseEntity;
import net.magnesiumbackend.core.exceptions.ExceptionHandlerRegistry;

import java.util.List;

/** A single registered route binding a method + route to a handler. */
public record RouteDefinition(HttpMethod mode, String path, HttpRouteHandler handler, List<HttpFilter> filters) {
    public ResponseEntity execute(
        RequestContext ctx,
        List<HttpFilter> globalFilters,
        ExceptionHandlerRegistry routeExceptionHandler
    ) {
        try {
            return new DefaultFilterChain(globalFilters, filters, handler)
                .next(ctx);
        } catch (Throwable throwable) {
            return routeExceptionHandler.resolve(ctx.request().routeDefinition(), throwable, ctx);
        }
    }
}