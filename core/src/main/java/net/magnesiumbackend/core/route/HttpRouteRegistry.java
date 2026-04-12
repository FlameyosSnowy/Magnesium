package net.magnesiumbackend.core.route;

import net.magnesiumbackend.core.http.response.HttpMethod;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registry for HTTP routes using unified handler and filter interfaces.
 *
 * <p>Both handlers and filters can return either synchronous results (ResponseEntity or raw values)
 * or asynchronous results (CompletableFuture). The actual type is resolved at runtime.</p>
 */
public class HttpRouteRegistry {

    private final EnumMap<HttpMethod, RouteTree<RouteDefinition>> trees =
        new EnumMap<>(HttpMethod.class);

    /**
     * Registers a route.
     *
     * @param method HTTP method
     * @param template Route path template
     * @param handler Route handler (can return ResponseEntity, raw value, or CompletableFuture)
     * @param filters List of filters (each can return ResponseEntity, raw value, or CompletableFuture)
     */
    public void register(HttpMethod method,
                         RoutePathTemplate template,
                         HttpRouteHandler handler,
                         List<HttpFilter> filters) {

        RouteDefinition def = new RouteDefinition(method, template.raw(), handler, filters);

        RouteTree<RouteDefinition> tree =
            trees.computeIfAbsent(method, _ -> new RouteTree<>());

        tree.register(template, def);
    }

    public Optional<RouteTree.RouteMatch<RouteDefinition>> find(HttpMethod method, String path) {
        RouteTree<RouteDefinition> tree = trees.get(method);
        if (tree == null) return Optional.empty();

        return tree.match(path);
    }

    public Map<HttpMethod, RouteTree<RouteDefinition>> trees() {
        return Collections.unmodifiableMap(trees);
    }

    public boolean isEmpty() {
        return trees.isEmpty();
    }

    public record MatchedRoute(RouteDefinition definition, Map<String, String> pathVariables) {}
}