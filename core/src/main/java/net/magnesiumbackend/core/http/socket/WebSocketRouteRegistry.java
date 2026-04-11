package net.magnesiumbackend.core.http.socket;

import net.magnesiumbackend.core.route.RoutePathTemplate;
import net.magnesiumbackend.core.route.RouteTree;

import javax.sound.midi.SysexMessage;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class WebSocketRouteRegistry {

    private final RouteTree<WebSocketHandler> tree = new RouteTree<>();

    public void register(String path, WebSocketHandler handler) {
        tree.register(RoutePathTemplate.compile(path), handler);
    }

    public Optional<RouteTree.RouteMatch<WebSocketHandler>> match(String path) {
        return tree.match(path);
    }

    public List<RouteTree.RouteDumpEntry<WebSocketHandler>> dump() {
        return tree.dump();
    }

    public Collection<? extends RouteTree.RouteEntry<WebSocketHandler>> entries() {
        return tree.entries();
    }
}