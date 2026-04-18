package net.magnesiumbackend.core;

import net.magnesiumbackend.core.http.response.*;
import net.magnesiumbackend.core.http.websocket.*;
import net.magnesiumbackend.core.route.*;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class Router {

    private final HttpRouteRegistry routes = new HttpRouteRegistry();
    private final List<HttpFilter> globalFilters = new ArrayList<>(8);
    private final WebSocketRouteRegistry webSocketRouteRegistry = new WebSocketRouteRegistry();

    private boolean frozen = false;
    private final WebSocketSessionManager webSocketSessionManager;

    public Router() {
        this.webSocketSessionManager = WebSocketSessionManagerLoader.load().orElse(null);
    }

    public @NotNull RouteBuilder route(HttpMethod method, String path, HttpRouteHandler handler) {
        ensureMutable();

        RouteBuilder builder = new RouteBuilder(method, path, handler);
        builder.attach(this);
        return builder;
    }

    public RouteBuilder get(String path, HttpRouteHandler handler) {
        return route(HttpMethod.GET, path, handler);
    }

    public RouteBuilder post(String path, HttpRouteHandler handler) {
        return route(HttpMethod.POST, path, handler);
    }

    public RouteBuilder put(String path, HttpRouteHandler handler) {
        return route(HttpMethod.PUT, path, handler);
    }

    public RouteBuilder patch(String path, HttpRouteHandler handler) {
        return route(HttpMethod.PATCH, path, handler);
    }

    public RouteBuilder delete(String path, HttpRouteHandler handler) {
        return route(HttpMethod.DELETE, path, handler);
    }

    public RouteBuilder head(String path, HttpRouteHandler handler) {
        return route(HttpMethod.HEAD, path, handler);
    }

    public RouteBuilder options(String path, HttpRouteHandler handler) {
        return route(HttpMethod.OPTIONS, path, handler);
    }

    public RouteBuilder trace(String path, HttpRouteHandler handler) {
        return route(HttpMethod.TRACE, path, handler);
    }

    public RouteBuilder connect(String path, HttpRouteHandler handler) {
        return route(HttpMethod.CONNECT, path, handler);
    }

    public Router filter(HttpFilter filter) {
        ensureMutable();
        globalFilters.add(filter);
        return this;
    }

    public Router websocket(String path, WebSocketHandler handler) {
        ensureMutable();
        webSocketRouteRegistry.register(path, handler);
        return this;
    }

    public Router websocket(String path, Consumer<WebSocketRouteBuilder> config) {
        ensureMutable();
        WebSocketRouteBuilder builder = new WebSocketRouteBuilder();
        config.accept(builder);
        webSocketRouteRegistry.register(path, builder.build());
        return this;
    }

    void freeze() {
        this.frozen = true;
    }

    private void ensureMutable() {
        if (frozen) {
            throw new IllegalStateException("Router is frozen");
        }
    }

    public HttpRouteRegistry routes() {
        return routes;
    }

    public List<HttpFilter> globalFilters() {
        return globalFilters;
    }

    public WebSocketRouteRegistry webSocketRouteRegistry() {
        return webSocketRouteRegistry;
    }

    public boolean isConfigured() {
        return !globalFilters.isEmpty() || !routes.isEmpty();
    }

    public WebSocketSessionManager webSocketSessionManager() {
        return webSocketSessionManager;
    }

    public static final class RouteBuilder {

        private final HttpMethod method;
        private final String path;
        private final HttpRouteHandler handler;

        private final List<HttpFilter> filters = new ArrayList<>(4);
        private Router router;

        private RouteBuilder(HttpMethod method, String path, HttpRouteHandler handler) {
            this.method = method;
            this.path = path;
            this.handler = handler;
        }

        private void attach(Router router) {
            this.router = router;
        }

        public RouteBuilder filter(HttpFilter filter) {
            filters.add(filter);
            return this;
        }

        public Router timeout(long value, TimeUnit unit) {
            // attach metadata into route definition later
            commit();
            return router;
        }

        public Router commit() {
            router.routes.register(
                method,
                RoutePathTemplate.compile(path.getBytes(StandardCharsets.UTF_8)),
                handler,
                List.copyOf(filters)
            );
            return router;
        }
    }
}