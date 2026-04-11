package net.magnesiumbackend.core.http.socket;

public interface WebSocketMessage {

    boolean isText();

    boolean isBinary();

    String asText();

    byte[] asBinary();
}