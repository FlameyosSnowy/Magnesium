package net.magnesiumbackend.core.http.websocket;

public class DefaultWebSocketMessage implements WebSocketMessage {

    private final String text;
    private final byte[] binary;

    private DefaultWebSocketMessage(String text, byte[] binary) {
        this.text = text;
        this.binary = binary;
    }

    public static DefaultWebSocketMessage ofText(String text) {
        return new DefaultWebSocketMessage(text, null);
    }

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