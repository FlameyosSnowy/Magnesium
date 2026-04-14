package net.magnesiumbackend.transport.httpserver;

import com.sun.net.httpserver.HttpExchange;
import net.magnesiumbackend.core.headers.HttpPathParamIndex;
import net.magnesiumbackend.core.http.websocket.WebSocketHandlerWrapper;
import net.magnesiumbackend.core.http.websocket.WebSocketSessionManager;
import robaho.net.httpserver.websockets.WebSocket;

import java.util.Map;

public class HttpServerWebSocketHandler extends robaho.net.httpserver.websockets.WebSocketHandler {
    private final WebSocketHandlerWrapper magnesiumHandler;
    private final WebSocketSessionManager sessionManager;
    private final String path;
    private final HttpPathParamIndex pathVariables;

    public HttpServerWebSocketHandler(
        WebSocketHandlerWrapper magnesiumHandler,
        WebSocketSessionManager sessionManager,
        String path,
        HttpPathParamIndex pathVariables
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