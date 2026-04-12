package net.magnesiumbackend.transport.tomcat.websocket;

import jakarta.websocket.Session;
import net.magnesiumbackend.core.http.websocket.WebSocketSession;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

public class TomcatWebSocketSession implements WebSocketSession {
    private static final Logger LOGGER = LoggerFactory.getLogger(TomcatWebSocketSession.class);

    private final Session session;
    private final Map<String, String> pathVariables;
    private final Map<String, String> headers;

    public TomcatWebSocketSession(
        Session session,
        Map<String, String> pathVariables,
        Map<String, String> headers
    ) {
        this.session       = session;
        this.pathVariables = pathVariables;
        this.headers       = headers;
    }

    @Override
    public String id() {
        return session.getId();
    }

    @Override
    public void sendText(@NotNull String text) {
        try {
            session.getBasicRemote().sendText(text);
        } catch (IOException e) {
            LOGGER.error("Failed to send text frame", e);
        }
    }

    @Override
    public void sendBinary(byte @NotNull [] bytes) {
        try {
            session.getBasicRemote().sendBinary(ByteBuffer.wrap(bytes));
        } catch (IOException e) {
            LOGGER.error("Failed to send binary frame", e);
        }
    }

    @Override
    public void close() {
        try {
            session.close();
        } catch (IOException e) {
            LOGGER.error("Failed to close session", e);
        }
    }

    @Override
    public void close(int code, @NotNull String reason) {
        try {
            session.close(new jakarta.websocket.CloseReason(
                jakarta.websocket.CloseReason.CloseCodes.getCloseCode(code), reason
            ));
        } catch (IOException e) {
            LOGGER.error("Failed to close session with code", e);
        }
    }

    @Override
    public boolean isOpen() {
        return session.isOpen();
    }

    @Override
    public Map<String, String> pathVariables() {
        return pathVariables;
    }

    @Override
    public Map<String, String> headers() {
        return headers;
    }
}