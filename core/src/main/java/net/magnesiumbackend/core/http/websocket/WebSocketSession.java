package net.magnesiumbackend.core.http.websocket;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface WebSocketSession {

    String id();

    void close(int code, @NotNull String reason);

    boolean isOpen();

    Map<String, String> pathVariables();

    Map<String, String> headers();

    void sendText(String text);

    void sendBinary(byte[] data);

    void close();
}