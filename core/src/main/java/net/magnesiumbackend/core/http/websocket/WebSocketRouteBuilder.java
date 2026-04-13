package net.magnesiumbackend.core.http.websocket;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Builder for creating {@link WebSocketHandler} instances using lambdas.
 *
 * <p>WebSocketRouteBuilder provides a fluent API for creating WebSocket
 * handlers without implementing the full interface. Useful for simple
 * handlers or inline definitions.</p>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * WebSocketHandler handler = new WebSocketRouteBuilder()
 *     .onOpen(session -> {
 *         System.out.println("Connected: " + session.id());
 *     })
 *     .onMessage((session, message) -> {
 *         session.sendText("Echo: " + message.asText());
 *     })
 *     .onClose((session, code, reason) -> {
 *         System.out.println("Closed: " + reason);
 *     })
 *     .build();
 * }</pre>
 *
 * @see WebSocketHandler
 * @see WebSocketRouteRegistry
 */
public final class WebSocketRouteBuilder {

    private Consumer<WebSocketSession> onOpen = s -> {};
    private BiConsumer<WebSocketSession, WebSocketMessage> onMessage = (s, m) -> {};
    private OnClose onClose = (s, c, r) -> {};
    private BiConsumer<WebSocketSession, Throwable> onError = (s, e) -> {};

    /**
     * Sets the open handler.
     *
     * @param fn the handler for new connections
     * @return this builder
     */
    public WebSocketRouteBuilder onOpen(Consumer<WebSocketSession> fn) {
        this.onOpen = fn;
        return this;
    }

    /**
     * Sets the message handler.
     *
     * @param fn the handler for incoming messages
     * @return this builder
     */
    public WebSocketRouteBuilder onMessage(BiConsumer<WebSocketSession, WebSocketMessage> fn) {
        this.onMessage = fn;
        return this;
    }

    /**
     * Sets the close handler.
     *
     * @param fn the handler for closed connections
     * @return this builder
     */
    public WebSocketRouteBuilder onClose(OnClose fn) {
        this.onClose = fn;
        return this;
    }

    /**
     * Sets the error handler.
     *
     * @param fn the handler for errors
     * @return this builder
     */
    public WebSocketRouteBuilder onError(BiConsumer<WebSocketSession, Throwable> fn) {
        this.onError = fn;
        return this;
    }

    /**
     * Builds the WebSocket handler.
     *
     * @return a new WebSocketHandler instance
     */
    @Contract(value = " -> new", pure = true)
    @NotNull
    public WebSocketHandler build() {
        return new WebSocketHandler() {
            @Override public void onOpen(WebSocketSession s) { onOpen.accept(s); }
            @Override public void onMessage(WebSocketSession s, WebSocketMessage m) { onMessage.accept(s, m); }
            @Override public void onClose(WebSocketSession s, int c, String r) { onClose.accept(s, c, r); }
            @Override public void onError(WebSocketSession s, Throwable e) { onError.accept(s, e); }
        };
    }

    /**
     * Functional interface for close event handling.
     */
    @FunctionalInterface
    public interface OnClose {
        /**
         * Called when a WebSocket connection closes.
         *
         * @param session the closing session
         * @param code    the close status code
         * @param reason  the close reason
         */
        void accept(WebSocketSession session, int code, String reason);
    }
}