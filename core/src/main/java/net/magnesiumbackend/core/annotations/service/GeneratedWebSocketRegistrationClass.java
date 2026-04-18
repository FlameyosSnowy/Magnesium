package net.magnesiumbackend.core.annotations.service;

import net.magnesiumbackend.core.MagnesiumRuntime;
import net.magnesiumbackend.core.http.websocket.WebSocketRouteRegistry;
import net.magnesiumbackend.core.services.ServiceRegistry;

/**
 * Implemented by every compile-time-generated WebSocket route registration class.
 *
 * <p>The annotation processor generates one concrete implementation per
 * {@code @WebSocketMapping} method found in the project. Each generated class
 * registers WebSocket handlers with the application's {@link WebSocketRouteRegistry}
 * at startup.</p>
 *
 * <p>The generated class wires together path patterns, handler factories,
 * and dependency injection to enable type-safe WebSocket endpoints.</p>
 *
 * @see net.magnesiumbackend.core.http.websocket.WebSocketRouteRegistry
 * @see net.magnesiumbackend.core.http.websocket.WebSocketHandler
 */
public interface GeneratedWebSocketRegistrationClass {
    /**
     * Registers all WebSocket routes defined in the associated controller.
     *
     * <p>This method is called once during application startup. It instantiates
     * handlers and registers them with the webSocketRouteRegistry.</p>
     *
     * @param application            the running application
     * @param serviceRegistry        used to resolve handler constructor dependencies
     * @param webSocketRouteRegistry destination for WebSocket route registrations
     */
    void register(
        MagnesiumRuntime application,
        ServiceRegistry serviceRegistry,
        WebSocketRouteRegistry webSocketRouteRegistry
    );
}