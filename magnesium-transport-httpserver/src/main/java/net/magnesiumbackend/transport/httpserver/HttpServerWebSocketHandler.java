package net.magnesiumbackend.transport.httpserver;

import com.sun.net.httpserver.HttpExchange;
import net.magnesiumbackend.core.http.websocket.WebSocketHandler;
import net.magnesiumbackend.core.http.websocket.WebSocketSessionManager;
import robaho.net.httpserver.websockets.WebSocket;

import java.util.Map;

public class HttpServerWebSocketHandler extends robaho.net.httpserver.websockets.WebSocketHandler {
    private final WebSocketHandler magnesiumHandler;
    private final WebSocketSessionManager sessionManager;
    private final String path;
    private final Map<String, String> pathVariables;

    public HttpServerWebSocketHandler(
        WebSocketHandler magnesiumHandler,
        WebSocketSessionManager sessionManager,
        String path,
        Map<String, String> pathVariables
    ) {
        this.magnesiumHandler = magnesiumHandler;
        this.sessionManager   = sessionManager;
        this.path             = path;
        this.pathVariables    = pathVariables;
    }

    @Override
    protected WebSocket openWebSocket(HttpExchange exchange) {
        return new MagnesiumWebSocket(
            exchange,
            magnesiumHandler,
            sessionManager,
            path,
            pathVariables
        );
    }
}