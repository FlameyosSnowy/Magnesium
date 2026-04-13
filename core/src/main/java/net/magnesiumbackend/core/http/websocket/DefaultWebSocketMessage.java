package net.magnesiumbackend.core.http.websocket;

/**
 * Default implementation of {@link WebSocketMessage}.
 *
 * <p>Simple immutable implementation that stores either text or binary
 * content. Factory methods {@link #ofText(String)} and
 * {@link #ofBinary(byte[])} create appropriately typed instances.</p>
 *
 * @see WebSocketMessage
 */
public class DefaultWebSocketMessage implements WebSocketMessage {

    private final String text;
    private final byte[] binary;

    private DefaultWebSocketMessage(String text, byte[] binary) {
        this.text = text;
        this.binary = binary;
    }

    /**
     * Creates a text WebSocket message.
     *
     * @param text the text content
     * @return a new text message
     */
    public static DefaultWebSocketMessage ofText(String text) {
        return new DefaultWebSocketMessage(text, null);
    }

    /**
     * Creates a binary WebSocket message.
     *
     * @param binary the binary content
     * @return a new binary message
     */
    public static DefaultWebSocketMessage ofBinary(byte[] binary) {
        return new DefaultWebSocketMessage(null, binary);
    }

    @Override public boolean isText()   { return text != null; }
    @Override public boolean isBinary() { return binary != null; }

    @Override
    public String asText() {
        if (!isText()) throw new IllegalStateException("Message is binary, not text");
        return text;
    }

    @Override
    public byte[] asBinary() {
        if (!isBinary()) throw new IllegalStateException("Message is text, not binary");
        return binary;
    }
}