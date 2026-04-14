package net.magnesiumbackend.transport.tomcat.websocket;

import jakarta.websocket.Session;
import net.magnesiumbackend.core.headers.HttpHeaderIndex;
import net.magnesiumbackend.core.headers.HttpPathParamIndex;
import net.magnesiumbackend.core.http.websocket.WebSocketSession;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;

public class TomcatWebSocketSession implements WebSocketSession {
    private static final Logger LOGGER = LoggerFactory.getLogger(TomcatWebSocketSession.class);

    private final Session session;
    private final HttpPathParamIndex pathVariables;
    private final HttpHeaderIndex headers;

    public TomcatWebSocketSession(
        Session session,
        HttpPathParamIndex pathVariables,
        HttpHeaderIndex headers
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
    public void sendTextInIoThread(String text) {
        try {
            session.getBasicRemote().sendText(text);
        } catch (IOException e) {
            LOGGER.error("Failed to send text frame", e);
        }
    }

    @Override
    public void sendBinaryInIoThread(byte[] data) {
        try {
            session.getBasicRemote().sendBinary(ByteBuffer.wrap(data));
        } catch (IOException e) {
            LOGGER.error("Failed to send binary frame", e);
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
    public HttpPathParamIndex pathVariables() {
        return pathVariables;
    }

    @Override
    public HttpHeaderIndex headers() {
        return headers;
    }
}