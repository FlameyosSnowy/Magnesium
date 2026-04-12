package net.magnesiumbackend.core.route;

import net.magnesiumbackend.core.http.response.HttpMethod;
import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.exceptions.ExceptionHandlerRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * A single registered route binding a method + route to a handler.
 *
 * <p>Uses unified {@link HttpRouteHandler} and {@link HttpFilter} interfaces that support
 * both synchronous and asynchronous operations. The return type is resolved at runtime
 * using {@link ResponseEntityResolver}.</p>
 *
 * <p>Handlers and filters can return:
 * <ul>
 *   <li>{@code ResponseEntity<T>} - Synchronous response</li>
 *   <li>{@code T} (any object) - Synchronous response, auto-wrapped</li>
 *   <li>{@code CompletableFuture<ResponseEntity<T>>} - Asynchronous response</li>
 *   <li>{@code CompletableFuture<T>} - Asynchronous response, auto-wrapped</li>
 * </ul>
 */
public record RouteDefinition(
    HttpMethod mode,
    String path,
    HttpRouteHandler handler,
    List<HttpFilter> filters
) {
    /**
     * Creates a route definition.
     */
    public RouteDefinition(HttpMethod mode, String path, HttpRouteHandler handler, List<HttpFilter> filters) {
        this.mode = mode;
        this.path = path;
        this.handler = handler;
        this.filters = filters != null ? filters : List.of();
    }

    /**
     * Executes the route synchronously.
     *
     * @param ctx The request context
     * @param globalFilters Global filters to apply
     * @param routeExceptionHandler Exception handler for the route
     * @return The response entity
     */
    public ResponseEntity<?> execute(
        RequestContext ctx,
        List<HttpFilter> globalFilters,
        ExceptionHandlerRegistry routeExceptionHandler
    ) {
        try {
            List<HttpFilter> allFilters = new ArrayList<>();
            allFilters.addAll(globalFilters);
            allFilters.addAll(filters);

            return new DefaultFilterChain(allFilters, List.of(), handler).next(ctx);
        } catch (Throwable throwable) {
            return routeExceptionHandler.resolve(ctx.request().routeDefinition(), throwable, ctx);
        }
    }

    /**
     * Executes the route asynchronously.
     *
     * @param ctx The request context
     * @param globalFilters Global filters to apply
     * @param routeExceptionHandler Exception handler for the route
     * @return A CompletableFuture that completes with the response entity
     */
    public CompletableFuture<ResponseEntity<?>> executeAsync(
        RequestContext ctx,
        List<HttpFilter> globalFilters,
        ExceptionHandlerRegistry routeExceptionHandler,
        Executor requestExecutor
    ) {
        return CompletableFuture.supplyAsync(() -> {

                try {
                    List<HttpFilter> allFilters = new ArrayList<>(globalFilters);
                    allFilters.addAll(filters);

                    FilterChain chain = createAsyncFilterChain(
                        allFilters,
                        handler,
                        ctx,
                        routeExceptionHandler
                    );

                    return chain.next(ctx);

                } catch (Throwable t) {
                    throw new CompletionException(t);
                }

            }, requestExecutor != null ? requestExecutor : ForkJoinPool.commonPool())
            .thenCompose(ResponseEntityResolver::toCompletableFuture)
            .exceptionallyCompose(throwable ->
                routeExceptionHandler.resolveAsync(
                    ctx.request().routeDefinition(),
                    throwable,
                    ctx
                )
            );
    }

    public CompletableFuture<ResponseEntity<?>> executeAsync(
        RequestContext ctx,
        List<HttpFilter> globalFilters,
        ExceptionHandlerRegistry routeExceptionHandler
    ) {
        return executeAsync(ctx, globalFilters, routeExceptionHandler, ForkJoinPool.commonPool());
    }



    /**
     * Creates a filter chain that handles async results.
     */
    private FilterChain createAsyncFilterChain(
        List<HttpFilter> filters,
        HttpRouteHandler handler,
        RequestContext ctx,
        ExceptionHandlerRegistry exceptionHandler
    ) {
        return new FilterChain() {
            private int index = 0;

            @Override
            public ResponseEntity<?> next(RequestContext ctx) {
                if (index < filters.size()) {
                    Object result = filters.get(index++).handle(ctx, this);
                    // Resolve async filters and continue chain
                    if (result instanceof CompletableFuture) {
                        throw new IllegalStateException("Async filter results should be handled by AsyncResolver");
                    }
                    return ResponseEntityResolver.resolveSync(result);
                }
                // Execute handler
                Object result = handler.handle(ctx);
                return ResponseEntityResolver.resolveSync(result);
            }
        };
    }
}