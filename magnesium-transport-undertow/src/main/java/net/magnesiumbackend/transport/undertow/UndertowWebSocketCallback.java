package net.magnesiumbackend.transport.undertow;

import io.undertow.server.HttpServerExchange;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import net.magnesiumbackend.core.http.socket.WebSocketHandler;
import net.magnesiumbackend.core.http.socket.WebSocketSessionManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UndertowWebSocketCallback implements WebSocketConnectionCallback {
    private static final Logger LOGGER = LoggerFactory.getLogger(UndertowWebSocketCallback.class);
    private final WebSocketHandler handler;
    private final WebSocketSessionManager sessionManager;
    private final String path;
    private final Map<String, String> pathVariables;

    public UndertowWebSocketCallback(
        WebSocketHandler handler,
        WebSocketSessionManager sessionManager,
        String path,
        Map<String, String> pathVariables
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
            handler.onOpen(session);
        } catch (Exception e) {
            try {
                channel.close();
            } catch (IOException ex) {
                LOGGER.error("Failed to close channel", ex);
            }
        }
    }

    private @NotNull UndertowWebSocketSession setupHeaders(WebSocketHttpExchange exchange, WebSocketChannel channel) {
        Map<String, List<String>> requestHeaders = exchange.getRequestHeaders();
        Map<String, String> headers = new HashMap<>(requestHeaders.size());
        for (Map.Entry<String, List<String>> entry : requestHeaders.entrySet()) {
            String name = entry.getKey();
            List<String> values = entry.getValue();
            if (!values.isEmpty()) headers.put(name, values.getFirst());
        }

        UndertowWebSocketSession session = new UndertowWebSocketSession(
            channel, pathVariables, headers
        );
        return session;
    }
}