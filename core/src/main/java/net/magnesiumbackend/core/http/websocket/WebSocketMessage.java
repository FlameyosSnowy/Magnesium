package net.magnesiumbackend.core.http.websocket;

/**
 * Represents a WebSocket message (text or binary).
 *
 * <p>WebSocketMessage abstracts over text and binary WebSocket frames,
 * providing type-checking methods and conversion accessors. Messages are
 * either text or binary, not both.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * @Override
 * public void onMessage(WebSocketSession session, WebSocketMessage message) {
 *     if (message.isText()) {
 *         String text = message.asText();
 *         // Process text message
 *     } else {
 *         byte[] data = message.asBinary();
 *         // Process binary data
 *     }
 * }
 * }</pre>
 *
 * @see DefaultWebSocketMessage
 * @see WebSocketHandler#onMessage(WebSocketSession, WebSocketMessage)
 */
public interface WebSocketMessage {

    /**
     * Returns true if this is a text message.
     *
     * @return true for text messages
     */
    boolean isText();

    /**
     * Returns true if this is a binary message.
     *
     * @return true for binary messages
     */
    boolean isBinary();

    /**
     * Returns the text content.
     *
     * @return the text payload
     * @throws IllegalStateException if this is a binary message
     */
    String asText();

    /**
     * Returns the binary content.
     *
     * @return the binary payload
     * @throws IllegalStateException if this is a text message
     */
    byte[] asBinary();
}