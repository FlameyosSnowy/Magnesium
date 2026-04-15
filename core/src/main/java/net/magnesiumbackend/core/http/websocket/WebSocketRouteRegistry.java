package net.magnesiumbackend.core.http.websocket;

import net.magnesiumbackend.core.headers.Slice;
import net.magnesiumbackend.core.route.RoutePathTemplate;
import net.magnesiumbackend.core.route.RouteTree;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

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
        tree.register(RoutePathTemplate.compile(path.getBytes(StandardCharsets.UTF_8)), new WebSocketHandlerWrapper(handler));
    }

    public RouteTree<WebSocketHandlerWrapper> tree() {
        return tree;
    }

    public RouteTree.RouteMatch<WebSocketHandlerWrapper> match(String path) {
        return tree.match(path.getBytes(StandardCharsets.UTF_8));
    }

    public RouteTree.RouteMatch<WebSocketHandlerWrapper> match(Slice path) {
        return tree.match(path.src());
    }

    public RouteTree.RouteMatch<WebSocketHandlerWrapper> match(byte[] path) {
        return tree.match(path);
    }

    public List<RouteTree.RouteDumpEntry<WebSocketHandlerWrapper>> dump() {
        return tree.dump();
    }

    public Collection<? extends RouteTree.RouteEntry<WebSocketHandlerWrapper>> entries() {
        return tree.entries();
    }
}