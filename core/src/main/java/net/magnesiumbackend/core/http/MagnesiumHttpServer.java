package net.magnesiumbackend.core.http;

import net.magnesiumbackend.core.WebSocketSessionManagerLoader;
import net.magnesiumbackend.core.http.websocket.WebSocketHandler;
import net.magnesiumbackend.core.http.websocket.WebSocketRouteBuilder;
import net.magnesiumbackend.core.http.websocket.WebSocketRouteRegistry;
import net.magnesiumbackend.core.http.websocket.WebSocketSessionManager;
import net.magnesiumbackend.core.route.HttpRouteRegistry;
import net.magnesiumbackend.core.route.HttpFilter;
import net.magnesiumbackend.core.route.HttpRouteHandler;
import net.magnesiumbackend.core.route.RoutePathTemplate;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Represents a configured HTTP server with a fixed set of routes.
 *
 * <p>Instances are created exclusively via {@link Builder} and are immutable
 * once {@link Builder#build()} is called.
 *
 * <pre>{@code
 * MagnesiumHttpServer server = MagnesiumHttpServer.builder()
 *     .route(RouteMode.GET, "/health",  req -> Response.ok("up"))
 *     .route(RouteMode.GET, "/version", req -> Response.ok(BuildInfo.VERSION))
 *     .build();
 * }</pre>
 */
public class MagnesiumHttpServer {
    private final HttpRouteRegistry httpRouteRegistry;
    private final List<HttpFilter> globalFilters;
    private final WebSocketRouteRegistry websocketRoutes;
    private final WebSocketSessionManager webSocketSessionManager;

    private MagnesiumHttpServer(Builder builder) {
        this.httpRouteRegistry = builder.routes;
        this.globalFilters = builder.globalFilters;
        this.websocketRoutes = builder.websocketRoutes;
        this.webSocketSessionManager = WebSocketSessionManagerLoader.load().orElse(null);
    }

    public HttpRouteRegistry routes() {
        return httpRouteRegistry;
    }

    @Contract(" -> new")
    public static @NotNull Builder builder() {
        return new Builder();
    }

    public boolean isConfigured() {
        return !httpRouteRegistry.isEmpty();
    }

    public WebSocketRouteRegistry webSocketRouteRegistry() {
        return websocketRoutes;
    }

    public WebSocketSessionManager webSocketSessionManager() {
        return webSocketSessionManager;
    }

    public static final class Builder {

        private final HttpRouteRegistry routes = new HttpRouteRegistry();

        private final List<HttpFilter> globalFilters = new ArrayList<>();
        private final WebSocketRouteRegistry websocketRoutes = new WebSocketRouteRegistry();

        private Builder() {}

        /**
         * Registers a route.
         *
         * @param mode    HTTP method (GET, POST, …)
         * @param path    URI route pattern, e.g. {@code "/users/{id}"}
         * @param handler function that receives a {@link Request} and returns a {@link ResponseEntity}
         * @return this builder, for chaining
         */
        public Builder route(HttpMethod mode, String path, HttpRouteHandler handler) {
            routes.register(mode, RoutePathTemplate.compile(path), handler, List.of());
            return this;
        }

        public Builder get(String path, HttpRouteHandler handler) {
            route(HttpMethod.GET, path, handler, List.of());
            return this;
        }

        public Builder post(String path, HttpRouteHandler handler) {
            route(HttpMethod.POST, path, handler, List.of());
            return this;
        }

        public Builder put(String path, HttpRouteHandler handler) {
            route(HttpMethod.PUT, path, handler, List.of());
            return this;
        }

        public Builder patch(String path, HttpRouteHandler handler) {
            route(HttpMethod.PATCH, path, handler, List.of());
            return this;
        }

        public Builder delete(String path, HttpRouteHandler handler) {
            route(HttpMethod.DELETE, path, handler, List.of());
            return this;
        }

        public Builder head(String path, HttpRouteHandler handler) {
            route(HttpMethod.HEAD, path, handler, List.of());
            return this;
        }

        public Builder options(String path, HttpRouteHandler handler) {
            route(HttpMethod.OPTIONS, path, handler, List.of());
            return this;
        }

        public Builder trace(String path, HttpRouteHandler handler) {
            route(HttpMethod.TRACE, path, handler, List.of());
            return this;
        }

        public Builder connect(String path, HttpRouteHandler handler) {
            route(HttpMethod.CONNECT, path, handler, List.of());
            return this;
        }

        /**
         * Registers a route.
         *
         * @param mode    HTTP method (GET, POST, …)
         * @param path    URI route pattern, e.g. {@code "/users/{id}"}
         * @param handler function that receives a {@link Request} and returns a {@link ResponseEntity}
         * @return this builder, for chaining
         */
        public Builder route(HttpMethod mode, String path, HttpRouteHandler handler, List<HttpFilter> filters) {
            routes.register(mode, RoutePathTemplate.compile(path), handler, filters);
            return this;
        }

        public Builder get(String path, HttpRouteHandler handler, List<HttpFilter> filters) {
            route(HttpMethod.GET, path, handler, filters);
            return this;
        }

        public Builder post(String path, HttpRouteHandler handler, List<HttpFilter> filters) {
            route(HttpMethod.POST, path, handler, filters);
            return this;
        }

        public Builder put(String path, HttpRouteHandler handler, List<HttpFilter> filters) {
            route(HttpMethod.PUT, path, handler, filters);
            return this;
        }

        public Builder patch(String path, HttpRouteHandler handler, List<HttpFilter> filters) {
            route(HttpMethod.PATCH, path, handler, filters);
            return this;
        }

        public Builder delete(String path, HttpRouteHandler handler, List<HttpFilter> filters) {
            route(HttpMethod.DELETE, path, handler, filters);
            return this;
        }

        public Builder head(String path, HttpRouteHandler handler, List<HttpFilter> filters) {
            route(HttpMethod.HEAD, path, handler, filters);
            return this;
        }

        public Builder options(String path, HttpRouteHandler handler, List<HttpFilter> filters) {
            route(HttpMethod.OPTIONS, path, handler, filters);
            return this;
        }

        public Builder trace(String path, HttpRouteHandler handler, List<HttpFilter> filters) {
            route(HttpMethod.TRACE, path, handler, filters);
            return this;
        }

        public Builder connect(String path, HttpRouteHandler handler, List<HttpFilter> filters) {
            route(HttpMethod.CONNECT, path, handler, filters);
            return this;
        }

        public Builder websocket(String path, WebSocketHandler handler) {
            websocketRoutes.register(path, handler);
            return this;
        }

        public Builder websocket(String path, Consumer<WebSocketRouteBuilder> config) {
            WebSocketRouteBuilder builder = new WebSocketRouteBuilder();
            config.accept(builder);
            websocketRoutes.register(path, builder.build());
            return this;
        }

        /** Builds the {@link MagnesiumHttpServer}. The builder must not be reused after this call. */
        @Contract(value = " -> new", pure = true)
        public @NotNull MagnesiumHttpServer build() {
            return new MagnesiumHttpServer(this);
        }
    }

    @Override
    public String toString() {
        return "MagnesiumHttpServer{" +
            "routes=" + httpRouteRegistry +
            '}';
    }

    public List<HttpFilter> globalFilters() {
        return globalFilters;
    }
}