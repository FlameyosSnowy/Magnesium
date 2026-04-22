package net.magnesiumbackend.test.route;

import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.route.DefaultFilterChain;
import net.magnesiumbackend.core.route.HttpFilter;
import net.magnesiumbackend.core.route.HttpRouteHandler;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests for DefaultFilterChain.
 */
class DefaultFilterChainTest {

    @Test
    void emptyChainExecutesHandler() {
        HttpRouteHandler handler = ctx -> ResponseEntity.ok("handler-result");
        DefaultFilterChain chain = new DefaultFilterChain(
            List.of(),
            List.of(),
            handler
        );

        ResponseEntity<?> result = chain.next(null);
        assertEquals("handler-result", result.body());
    }

    @Test
    void globalFiltersExecuteInOrder() {
        List<String> order = new ArrayList<>();

        HttpFilter filter1 = (ctx, chain) -> {
            order.add("filter1-before");
            Object result = chain.next(ctx);
            order.add("filter1-after");
            return result;
        };

        HttpFilter filter2 = (ctx, chain) -> {
            order.add("filter2-before");
            Object result = chain.next(ctx);
            order.add("filter2-after");
            return result;
        };

        HttpRouteHandler handler = ctx -> {
            order.add("handler");
            return ResponseEntity.ok("result");
        };

        DefaultFilterChain chain = new DefaultFilterChain(
            List.of(filter1, filter2),
            List.of(),
            handler
        );

        chain.next(null);

        assertEquals(List.of(
            "filter1-before",
            "filter2-before",
            "handler",
            "filter2-after",
            "filter1-after"
        ), order);
    }

    @Test
    void routeFiltersExecuteAfterGlobalFilters() {
        List<String> order = new ArrayList<>();

        HttpFilter globalFilter = (ctx, chain) -> {
            order.add("global");
            return chain.next(ctx);
        };

        HttpFilter routeFilter = (ctx, chain) -> {
            order.add("route");
            return chain.next(ctx);
        };

        HttpRouteHandler handler = ctx -> {
            order.add("handler");
            return ResponseEntity.ok("result");
        };

        DefaultFilterChain chain = new DefaultFilterChain(
            List.of(globalFilter),
            List.of(routeFilter),
            handler
        );

        chain.next(null);

        assertEquals(List.of("global", "route", "handler"), order);
    }

    @Test
    void filterCanShortCircuit() {
        List<String> order = new ArrayList<>();

        HttpFilter shortCircuitFilter = (ctx, chain) -> {
            order.add("short-circuit");
            return ResponseEntity.of(403, "blocked");
        };

        HttpRouteHandler handler = ctx -> {
            order.add("handler");
            return ResponseEntity.ok("result");
        };

        DefaultFilterChain chain = new DefaultFilterChain(
            List.of(shortCircuitFilter),
            List.of(),
            handler
        );

        ResponseEntity<?> result = chain.next(null);

        assertEquals("blocked", result.body());
        assertEquals(403, result.statusCode());
        assertFalse(order.contains("handler"));
    }

    @Test
    void filterCanModifyResponse() {
        HttpFilter modifierFilter = (ctx, chain) -> {
            Object result = chain.next(ctx);
            if (result instanceof ResponseEntity<?> entity) {
                return ResponseEntity.of(entity.statusCode(), entity.body() + "-modified");
            }
            throw new IllegalArgumentException("Expected ResponseEntity, got " + result);
        };

        HttpRouteHandler handler = ctx -> ResponseEntity.ok("original");

        DefaultFilterChain chain = new DefaultFilterChain(
            List.of(modifierFilter),
            List.of(),
            handler
        );

        ResponseEntity<?> result = chain.next(null);
        assertEquals("original-modified", result.body());
    }

    @Test
    void handlerThrowsException() {
        HttpRouteHandler handler = ctx -> {
            throw new RuntimeException("handler error");
        };

        DefaultFilterChain chain = new DefaultFilterChain(
            List.of(),
            List.of(),
            handler
        );

        assertThrows(RuntimeException.class, () -> chain.next(null));
    }

    @Test
    void filterThrowsException() {
        HttpFilter errorFilter = (ctx, chain) -> {
            throw new RuntimeException("filter error");
        };

        HttpRouteHandler handler = ctx -> ResponseEntity.ok("ok");

        DefaultFilterChain chain = new DefaultFilterChain(
            List.of(errorFilter),
            List.of(),
            handler
        );

        assertThrows(RuntimeException.class, () -> chain.next(null));
    }
}
