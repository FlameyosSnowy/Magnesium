package net.magnesiumbackend.transport.undertow.websocket;

import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import net.magnesiumbackend.core.headers.HttpHeaderIndex;
import net.magnesiumbackend.core.headers.HttpPathParamIndex;
import net.magnesiumbackend.core.http.websocket.WebSocketSession;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;

public class UndertowWebSocketSession implements WebSocketSession {
    private static final Logger LOGGER = LoggerFactory.getLogger(UndertowWebSocketSession.class);

    private final String id = UUID.randomUUID().toString();
    private final WebSocketChannel channel;
    private final HttpPathParamIndex pathVariables;
    private final HttpHeaderIndex headers;

    public UndertowWebSocketSession(
        WebSocketChannel channel,
        HttpPathParamIndex pathVariables,
        HttpHeaderIndex headers
    ) {
        this.channel       = channel;
        this.pathVariables = pathVariables;
        this.headers       = headers;
    }

    @Override
    public String id() { return id; }

    @Override
    public void sendText(@NotNull String text) {
        try {
            WebSockets.sendTextBlocking(text, channel);
        } catch (IOException e) {
            LOGGER.error("Failed to send text frame", e);
        }
    }

    @Override
    public void sendBinary(byte @NotNull [] bytes) {
        try {
            WebSockets.sendBinaryBlocking(ByteBuffer.wrap(bytes), channel);
        } catch (IOException e) {
            LOGGER.error("Failed to send binary frame", e);
        }
    }

    @Override
    public void close() {
        try {
            channel.close();
        } catch (IOException e) {
            LOGGER.error("Failed to close session", e);
        }
    }

    @Override
    public void sendTextInIoThread(String text) {
        try {
            WebSockets.sendTextBlocking(text, channel);
        } catch (IOException e) {
            LOGGER.error("Failed to send text frame", e);
        }
    }

    @Override
    public void sendBinaryInIoThread(byte[] data) {
        try {
            WebSockets.sendBinaryBlocking(ByteBuffer.wrap(data), channel);
        } catch (IOException e) {
            LOGGER.error("Failed to send binary frame", e);
        }
    }

    @Override
    public void close(int code, @NotNull String reason) {
        WebSockets.sendClose(code, reason, channel, null);
    }

    @Override
    public boolean isOpen() { return channel.isOpen(); }

    @Override
    public HttpPathParamIndex pathVariables() { return pathVariables; }

    @Override
    public HttpHeaderIndex headers() { return headers; }
}