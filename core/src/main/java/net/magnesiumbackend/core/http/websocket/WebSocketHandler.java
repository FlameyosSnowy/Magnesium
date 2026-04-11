package net.magnesiumbackend.core.http.websocket;

public interface WebSocketHandler {

    default void onOpen(WebSocketSession session) {}

    default void onMessage(WebSocketSession session, WebSocketMessage message) {}

    default void onClose(WebSocketSession session, int statusCode, String reason) {}

    default void onError(WebSocketSession session, Throwable error) {}
}