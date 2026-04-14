package net.magnesiumbackend.devtools.debug;

import net.magnesiumbackend.core.http.response.HttpMethod;
import net.magnesiumbackend.core.route.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A wrapper for HttpRouteRegistry that profiles route matching.
 *
 * <p>Captures timing data for route resolution to identify performance bottlenecks
 * in the routing tree traversal.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * HttpRouteRegistry original = new HttpRouteRegistry();
 * HttpRouteRegistry profiling = new ProfilingRouteRegistry(original);
 *
 * // Use profiling registry in your application
 * MagnesiumApplication.builder()
 *     .routeRegistry(profiling)
 *     ...
 * }</pre>
 */
public record ProfilingRouteRegistry(HttpRouteRegistry delegate) {

    public void register(HttpMethod method,
                         RoutePathTemplate template,
                         HttpRouteHandler handler,
                         List<HttpFilter> filters) {
        delegate.register(method, template, handler, filters);
    }

    public Optional<RouteTree.RouteMatch<RouteDefinition>> find(HttpMethod method, String path) {
        if (!MagnesiumDebugger.isEnabled()) {
            return delegate.find(method, path);
        }

        long start = System.nanoTime();
        try {
            Optional<RouteTree.RouteMatch<RouteDefinition>> result = delegate.find(method, path);

            long nanos = System.nanoTime() - start;
            String pattern = result
                .map(_ -> method + " " + path)
                .orElse(method + " " + path + " [NOT_FOUND]");
            MagnesiumDebugger.routeMatched(pattern, nanos);

            return result;
        } catch (Exception e) {
            MagnesiumDebugger.recordError(e);
            throw e;
        }
    }

    public Map<HttpMethod, RouteTree<RouteDefinition>> trees() {
        return delegate.trees();
    }

    public boolean isEmpty() {
        return delegate.isEmpty();
    }
}
