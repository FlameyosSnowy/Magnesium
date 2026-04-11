package net.magnesiumbackend.transport.tomcat;

import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;
import net.magnesiumbackend.core.http.websocket.DefaultWebSocketMessage;
import net.magnesiumbackend.core.http.websocket.WebSocketHandler;
import net.magnesiumbackend.core.http.websocket.WebSocketSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class TomcatWebSocketEndpoint extends Endpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(TomcatWebSocketEndpoint.class);

    private final WebSocketHandler handler;
    private final WebSocketSessionManager sessionManager;
    private final String path;
    private final Map<String, String> pathVariables;

    private TomcatWebSocketSession session;

    public TomcatWebSocketEndpoint(
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
    public void onOpen(Session jakartaSession, EndpointConfig config) {
        Map<String, String> headers = Map.of(); // headers not available post-handshake in JSR-356
        session = new TomcatWebSocketSession(jakartaSession, pathVariables, headers);
        sessionManager.add(path, session);

        jakartaSession.addMessageHandler(String.class,
            (MessageHandler.Whole<String>) text -> {
                try {
                    handler.onMessage(session, DefaultWebSocketMessage.ofText(text));
                } catch (Exception e) {
                    LOGGER.error("WebSocket onMessage error", e);
                }
            });

        jakartaSession.addMessageHandler(byte[].class,
            (MessageHandler.Whole<byte[]>) bytes -> {
                try {
                    handler.onMessage(session, DefaultWebSocketMessage.ofBinary(bytes));
                } catch (Exception e) {
                    LOGGER.error("WebSocket onMessage error", e);
                }
            });

        try {
            handler.onOpen(session);
        } catch (Exception e) {
            LOGGER.error("WebSocket onOpen error", e);
        }
    }

    @Override
    public void onClose(Session jakartaSession, CloseReason closeReason) {
        sessionManager.remove(path, session);
        try {
            handler.onClose(
                session,
                closeReason.getCloseCode().getCode(),
                closeReason.getReasonPhrase()
            );
        } catch (Exception e) {
            LOGGER.error("WebSocket onClose error", e);
        }
    }

    @Override
    public void onError(Session jakartaSession, Throwable cause) {
        LOGGER.error("WebSocket channel error", cause);
        try {
            handler.onError(session, cause);
        } catch (Exception e) {
            LOGGER.error("WebSocket onError handler threw", e);
        }
    }
}