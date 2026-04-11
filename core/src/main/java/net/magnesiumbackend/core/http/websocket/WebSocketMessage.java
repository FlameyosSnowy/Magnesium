package net.magnesiumbackend.core.http.websocket;

public interface WebSocketMessage {

    boolean isText();

    boolean isBinary();

    String asText();

    byte[] asBinary();
}