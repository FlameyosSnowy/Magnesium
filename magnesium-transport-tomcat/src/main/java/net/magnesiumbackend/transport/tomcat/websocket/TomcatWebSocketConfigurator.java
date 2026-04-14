package net.magnesiumbackend.transport.tomcat.websocket;

import jakarta.websocket.server.ServerEndpointConfig;
import net.magnesiumbackend.core.headers.HttpPathParamIndex;
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
        return (T) new TomcatWebSocketEndpoint(handler, sessionManager, path, HttpPathParamIndex.empty());
    }
}