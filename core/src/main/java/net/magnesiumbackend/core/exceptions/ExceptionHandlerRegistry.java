package net.magnesiumbackend.core.exceptions;

import io.github.flameyossnowy.velocis.tables.ConcurrentHashTable;
import io.github.flameyossnowy.velocis.tables.Table;
import net.magnesiumbackend.core.http.ResponseEntity;
import net.magnesiumbackend.core.route.RequestContext;
import net.magnesiumbackend.core.route.RouteDefinition;
import net.magnesiumbackend.core.route.RouteExceptionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores and resolves exception handlers by route.
 */
public final class ExceptionHandlerRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandlerRegistry.class);

    /** Global exception handlers (applied to any route) */
    private final Map<Class<? extends Throwable>, RouteExceptionHandler> globalHandlers = new ConcurrentHashMap<>();

    /** Route-specific exception handlers */
    private final Table<RouteDefinition, Class<? extends Throwable>, RouteExceptionHandler> routeHandlers = new ConcurrentHashTable<>();

    /** Fallback handler if nothing matches */
    private RouteExceptionHandler fallback = (ex, req) -> {
        LOGGER.error("Uncaught exception, request data: {}", req, ex);
        return ResponseEntity.of(500, "Internal Server Error");
    };

    /** Registers a global handler for an exception type */
    public <T extends Throwable> void registerGlobal(
        @NotNull Class<T> exceptionType,
        @NotNull RouteExceptionHandler handler
    ) {
        globalHandlers.put(exceptionType, handler);
    }

    /** Registers a route-specific handler for an exception type */
    public <T extends Throwable> void registerRoute(
        @NotNull RouteDefinition route,
        @NotNull Class<T> exceptionType,
        @NotNull RouteExceptionHandler handler
    ) {
        routeHandlers
            .put(route, exceptionType, handler);
    }

    /** Replaces fallback handler */
    public void setFallback(@NotNull RouteExceptionHandler fallback) {
        this.fallback = fallback;
    }

    /**
     * Resolves and invokes the best handler for the exception in the context of a route.
     *
     * Resolution order:
     * 1. Route-specific handler, most specific class first.
     * 2. Global handler, most specific class first.
     * 3. Fallback.
     */
    @NotNull
    public ResponseEntity resolve(
        @Nullable RouteDefinition route,
        @NotNull Throwable exception,
        @NotNull RequestContext request
    ) {
        Class<? extends Throwable> exceptionType = exception.getClass();

        if (route != null) {
            Map<Class<? extends Throwable>, RouteExceptionHandler> handlers = routeHandlers.row(route);
            if (!handlers.isEmpty()) {
                RouteExceptionHandler h = findMostSpecific(handlers, exceptionType);
                if (h != null) return h.handle(exception, request);
            }
        }

        RouteExceptionHandler h = findMostSpecific(globalHandlers, exceptionType);
        if (h != null) return h.handle(exception, request);

        return fallback.handle(exception, request);
    }

    /** Walks class hierarchy to find most specific handler */
    @Nullable
    private RouteExceptionHandler findMostSpecific(
        Map<Class<? extends Throwable>, RouteExceptionHandler> handlers,
        Class<?> exceptionType
    ) {
        boolean seen = false;
        Map.Entry<Class<? extends Throwable>, RouteExceptionHandler> best = null;

        for (Map.Entry<Class<? extends Throwable>, RouteExceptionHandler> entry : handlers.entrySet()) {
            if (entry.getKey().isAssignableFrom(exceptionType)) {
                if (!seen || hierarchyDepth(entry.getKey()) > hierarchyDepth(best.getKey())) {
                    seen = true;
                    best = entry;
                }
            }
        }

        return seen ? best.getValue() : null;
    }

    /** Counts superclasses up to Object */
    private static int hierarchyDepth(Class<?> type) {
        int depth = 0;
        Class<?> cursor = type;
        while (cursor != null) {
            depth++;
            cursor = cursor.getSuperclass();
        }
        return depth;
    }
}