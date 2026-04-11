package net.magnesiumbackend.transport.tomcat;

import jakarta.websocket.server.ServerEndpointConfig;
import net.magnesiumbackend.core.http.websocket.WebSocketHandler;
import net.magnesiumbackend.core.http.websocket.WebSocketSessionManager;

import java.util.Map;

public class TomcatWebSocketConfigurator extends ServerEndpointConfig.Configurator {
    private final WebSocketHandler handler;
    private final WebSocketSessionManager sessionManager;
    private final String path;

    public TomcatWebSocketConfigurator(
        WebSocketHandler handler,
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