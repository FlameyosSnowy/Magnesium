package net.magnesiumbackend.core.http.websocket;

import net.magnesiumbackend.core.route.RoutePathTemplate;
import net.magnesiumbackend.core.route.RouteTree;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Registry for WebSocket routes using unified {@link WebSocketHandler}.
 *
 * <p>The unified handler interface supports both synchronous and asynchronous
 * operations through its Object return type, resolved at runtime by
 * {@link WebSocketHandlerWrapper}.</p>
 *
 * @see WebSocketHandler
 * @see WebSocketHandlerWrapper
 */
public class WebSocketRouteRegistry {

    private final RouteTree<WebSocketHandlerWrapper> tree = new RouteTree<>();

    /**
     * Registers a WebSocket handler (sync or async).
     *
     * <p>Handlers can return:
     * <ul>
     *   <li>{@code null} or void for synchronous handling</li>
     *   <li>{@code CompletableFuture<Void>} for asynchronous handling</li>
     * </ul></p>
     *
     * @param path The WebSocket path
     * @param handler The unified WebSocket handler
     */
    public void register(String path, WebSocketHandler handler) {
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