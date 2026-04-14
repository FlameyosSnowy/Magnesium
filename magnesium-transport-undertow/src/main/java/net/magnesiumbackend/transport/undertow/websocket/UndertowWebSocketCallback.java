package net.magnesiumbackend.transport.undertow.websocket;

import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import net.magnesiumbackend.core.headers.HttpHeaderIndex;
import net.magnesiumbackend.core.headers.HttpPathParamIndex;
import net.magnesiumbackend.core.http.websocket.WebSocketHandlerWrapper;
import net.magnesiumbackend.core.http.websocket.WebSocketSessionManager;
import net.magnesiumbackend.transport.undertow.adapter.UndertowHeaderAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UndertowWebSocketCallback implements WebSocketConnectionCallback {
    private static final Logger LOGGER = LoggerFactory.getLogger(UndertowWebSocketCallback.class);
    private final WebSocketHandlerWrapper handler;
    private final WebSocketSessionManager sessionManager;
    private final String path;
    private final HttpPathParamIndex pathVariables;

    public UndertowWebSocketCallback(
        WebSocketHandlerWrapper handler,
        WebSocketSessionManager sessionManager,
        String path,
        HttpPathParamIndex pathVariables
    ) {
        this.handler        = handler;
        this.sessionManager = sessionManager;
        this.path           = path;
        this.pathVariables  = pathVariables;
    }

    @Override
    public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel channel) {
        UndertowWebSocketSession session = setupHeaders(exchange, channel);

        sessionManager.add(path, session);

        UndertowWebSocketListener listener = new UndertowWebSocketListener(
            handler, sessionManager, session, path
        );
        channel.getReceiveSetter().set(listener);
        channel.resumeReceives();

        try {
            handler.onOpen(session).join();
        } catch (Exception e) {
            try {
                channel.close();
            } catch (IOException ex) {
                LOGGER.error("Failed to close channel", ex);
            }
        }
    }

    private @NotNull UndertowWebSocketSession setupHeaders(WebSocketHttpExchange exchange, WebSocketChannel channel) {
        HttpHeaderIndex headers = UndertowHeaderAdapter.from(exchange);
        return new UndertowWebSocketSession(channel, pathVariables, headers);
    }
}