package net.magnesiumbackend.core.http.websocket;

import java.util.concurrent.CompletableFuture;

/**
 * Wrapper for {@link WebSocketHandler} providing async execution of void methods.
 *
 * <p>This wrapper executes handler methods and returns CompletableFuture<Void>
 * for consistent async handling across transports. The handler itself uses
 * void methods, but async operations should use {@link WebSocketSession} async APIs.</p>
 *
 * @see WebSocketHandler
 * @see WebSocketSession
 */
public record WebSocketHandlerWrapper(WebSocketHandler handler) {

    /**
     * Executes onOpen and returns a CompletableFuture.
     */
    public CompletableFuture<Void> onOpen(WebSocketSession session) {
        handler.onOpen(session);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Executes onMessage and returns a CompletableFuture.
     */
    public CompletableFuture<Void> onMessage(WebSocketSession session, WebSocketMessage message) {
        handler.onMessage(session, message);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Executes onClose and returns a CompletableFuture.
     */
    public CompletableFuture<Void> onClose(WebSocketSession session, int statusCode, String reason) {
        handler.onClose(session, statusCode, reason);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Executes onError and returns a CompletableFuture.
     */
    public CompletableFuture<Void> onError(WebSocketSession session, Throwable error) {
        handler.onError(session, error);
        return CompletableFuture.completedFuture(null);
    }
}
