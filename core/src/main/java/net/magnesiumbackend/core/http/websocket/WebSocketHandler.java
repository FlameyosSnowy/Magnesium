package net.magnesiumbackend.core.http.websocket;

/**
 * WebSocket handler interface with void return types.
 *
 * <p>For asynchronous operations, use the async methods on {@link WebSocketSession}:
 * <ul>
 *   <li>{@link WebSocketSession#sendTextAsync(String)}</li>
 *   <li>{@link WebSocketSession#sendBinaryAsync(byte[])}</li>
 *   <li>{@link WebSocketSession#closeAsync()}</li>
 * </ul></p>
 *
 * @see WebSocketSession
 */
public interface WebSocketHandler {

    /** Called when a new WebSocket connection is opened. */
    default void onOpen(WebSocketSession session) {}

    /** Called when a message is received from the client. */
    default void onMessage(WebSocketSession session, WebSocketMessage message) {}

    /** Called when the WebSocket connection is closed. */
    default void onClose(WebSocketSession session, int statusCode, String reason) {}

    /** Called when an error occurs on the WebSocket connection. */
    default void onError(WebSocketSession session, Throwable error) {}
}