package net.magnesiumbackend.test.route;

import net.magnesiumbackend.core.http.response.HttpMethod;
import net.magnesiumbackend.core.route.HttpRouteRegistry;
import net.magnesiumbackend.core.route.RoutePathTemplate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

class HttpRouteRegistryTest {

    @Test
    void emptyRegistryIsEmpty() {
        HttpRouteRegistry registry = new HttpRouteRegistry();
        assertTrue(registry.isEmpty());
    }

    @Test
    void registerRouteMakesNonEmpty() {
        HttpRouteRegistry registry = new HttpRouteRegistry();
        registry.register(HttpMethod.GET, RoutePathTemplate.compile("/test"), ctx -> "OK", List.of());
        assertFalse(registry.isEmpty());
    }
}
