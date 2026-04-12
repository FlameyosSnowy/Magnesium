package net.magnesiumbackend.transport.tomcat.websocket;

import jakarta.websocket.server.ServerEndpointConfig;
import net.magnesiumbackend.core.http.websocket.WebSocketHandlerWrapper;
import net.magnesiumbackend.core.http.websocket.WebSocketSessionManager;

import java.util.Map;

public class TomcatWebSocketConfigurator extends ServerEndpointConfig.Configurator {
    private final WebSocketHandlerWrapper handler;
    private final WebSocketSessionManager sessionManager;
    private final String path;

    public TomcatWebSocketConfigurator(
        WebSocketHandlerWrapper handler,
        WebSocketSessionManager sessionManager,
        String path
    ) {
        this.handler        = handler;
        this.sessionManager = sessionManager;
        this.path           = path;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getEndpointInstance(Class<T> endpointClass) {
        // JSR-356 gives us path variables via session.getPathParameters() at onOpen time
        // so we pass empty map here and let TomcatWebSocketEndpoint read them from the session
        return (T) new TomcatWebSocketEndpoint(handler, sessionManager, path, Map.of());
    }
}