package net.magnesiumbackend.transport.httpserver;

import net.magnesiumbackend.core.http.websocket.WebSocketSession;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import robaho.net.httpserver.websockets.CloseCode;
import robaho.net.httpserver.websockets.WebSocket;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class HttpServerWebSocketSession implements WebSocketSession {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerWebSocketSession.class);

    private final String id = UUID.randomUUID().toString();
    private final WebSocket socket;
    private final Map<String, String> pathVariables;
    private final Map<String, String> headers;

    public HttpServerWebSocketSession(
        WebSocket socket,
        Map<String, String> pathVariables,
        Map<String, String> headers
    ) {
        this.socket        = socket;
        this.pathVariables = pathVariables;
        this.headers       = headers;
    }

    @Override
    public String id() { return id; }

    @Override
    public void sendText(@NotNull String text) {
        try {
            socket.send(text);
        } catch (IOException e) {
            LOGGER.error("Failed to send text frame", e);
        }
    }

    @Override
    public void sendBinary(byte @NotNull [] bytes) {
        try {
            socket.send(bytes);
        } catch (IOException e) {
            LOGGER.error("Failed to send binary frame", e);
        }
    }

    @Override
    public void close() {
        try {
            socket.close(CloseCode.NormalClosure, "Closed", false);
        } catch (IOException e) {
            LOGGER.error("Failed to close session", e);
        }
    }

    @Override
    public void close(int code, @NotNull String reason) {
        try {
            socket.close(CloseCode.find(code), reason, false);
        } catch (IOException e) {
            LOGGER.error("Failed to close session with code", e);
        }
    }

    @Override
    public boolean isOpen() { return socket.isOpen(); }

    @Override
    public Map<String, String> pathVariables() { return pathVariables; }

    @Override
    public Map<String, String> headers() { return headers; }
}