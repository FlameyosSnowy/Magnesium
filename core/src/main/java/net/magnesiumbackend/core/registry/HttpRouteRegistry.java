package net.magnesiumbackend.core.registry;

import net.magnesiumbackend.core.http.HttpMethod;
import net.magnesiumbackend.core.route.HttpFilter;
import net.magnesiumbackend.core.route.RouteDefinition;
import net.magnesiumbackend.core.route.HttpRouteHandler;
import net.magnesiumbackend.core.route.RoutePathTemplate;
import net.magnesiumbackend.core.route.RouteTree;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class HttpRouteRegistry {

    private final EnumMap<HttpMethod, RouteTree<RouteDefinition>> trees =
        new EnumMap<>(HttpMethod.class);

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