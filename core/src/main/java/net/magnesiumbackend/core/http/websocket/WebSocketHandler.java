package net.magnesiumbackend.core.http.websocket;

/**
 * Handler interface for WebSocket connections.
 *
 * <p>WebSocketHandler defines callbacks for WebSocket lifecycle events:
 * connection open, message receive, connection close, and errors. All methods
 * have default no-op implementations so handlers only need to override
 * the events they care about.</p>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * @WebSocketMapping(path = "/chat/{roomId}")
 * public class ChatHandler implements WebSocketHandler {
 *     @Override
 *     public void onOpen(WebSocketSession session) {
 *         String roomId = session.pathVariables().get("roomId");
 *         chatService.join(roomId, session);
 *     }
 *
 *     @Override
 *     public void onMessage(WebSocketSession session, WebSocketMessage message) {
 *         chatService.broadcast(session, message.asText());
 *     }
 *
 *     @Override
 *     public void onClose(WebSocketSession session, int code, String reason) {
 *         chatService.leave(session);
 *     }
 * }
 * }</pre>
 *
 * @see WebSocketSession
 * @see WebSocketMessage
 * @see net.magnesiumbackend.core.annotations.WebSocketMapping
 */
public interface WebSocketHandler {

    /**
     * Called when a new WebSocket connection is opened.
     *
     * @param session the new WebSocket session
     */
    default void onOpen(WebSocketSession session) {}

    /**
     * Called when a message is received from the client.
     *
     * @param session the WebSocket session that received the message
     * @param message the received message (text or binary)
     */
    default void onMessage(WebSocketSession session, WebSocketMessage message) {}

    /**
     * Called when the WebSocket connection is closed.
     *
     * @param session    the WebSocket session being closed
     * @param statusCode the close status code
     * @param reason     the close reason
     */
    default void onClose(WebSocketSession session, int statusCode, String reason) {}

    /**
     * Called when an error occurs on the WebSocket connection.
     *
     * @param session the WebSocket session where the error occurred
     * @param error   the error that occurred
     */
    default void onError(WebSocketSession session, Throwable error) {}
}