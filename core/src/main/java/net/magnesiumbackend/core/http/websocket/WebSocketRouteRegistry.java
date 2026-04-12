package net.magnesiumbackend.core.http.websocket;

import net.magnesiumbackend.core.route.RoutePathTemplate;
import net.magnesiumbackend.core.route.RouteTree;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Registry for WebSocket routes supporting both sync and async handlers.
 */
public class WebSocketRouteRegistry {

    private final RouteTree<WebSocketHandlerWrapper> tree = new RouteTree<>();

    /**
     * Registers a synchronous WebSocket handler.
     *
     * @param path The WebSocket path
     * @param handler The sync handler
     */
    public void register(String path, WebSocketHandler handler) {
        tree.register(RoutePathTemplate.compile(path), new WebSocketHandlerWrapper(handler));
    }

    /**
     * Registers an asynchronous WebSocket handler.
     *
     * @param path The WebSocket path
     * @param handler The async handler
     */
    public void registerAsync(String path, AsyncWebSocketHandler handler) {
        tree.register(RoutePathTemplate.compile(path), new WebSocketHandlerWrapper(handler));
    }

    public Optional<RouteTree.RouteMatch<WebSocketHandlerWrapper>> match(String path) {
        return tree.match(path);
    }

    public List<RouteTree.RouteDumpEntry<WebSocketHandlerWrapper>> dump() {
        return tree.dump();
    }

    public Collection<? extends RouteTree.RouteEntry<WebSocketHandlerWrapper>> entries() {
        return tree.entries();
    }
}