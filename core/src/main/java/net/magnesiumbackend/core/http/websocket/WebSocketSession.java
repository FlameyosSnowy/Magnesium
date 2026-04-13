package net.magnesiumbackend.core.http.websocket;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Represents a WebSocket connection session.
 *
 * <p>WebSocketSession provides methods for interacting with a WebSocket
 * connection including sending messages (text or binary), closing the
 * connection, and accessing request metadata like path variables and headers.
 * </p>
 *
 * <h3>Send Methods</h3>
 * <ul>
 *   <li>{@link #sendText(String)} - Synchronous text send</li>
 *   <li>{@link #sendBinary(byte[])} - Synchronous binary send</li>
 *   <li>{@link #sendTextAsync(Supplier)} - Asynchronous text send</li>
 *   <li>{@link #sendBinaryAsync(Supplier)} - Asynchronous binary send</li>
 *   <li>{@link #sendTextInIoThread(String)} - Non-blocking for reactive transports</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * @Override
 * public void onMessage(WebSocketSession session, WebSocketMessage message) {
 *     String reply = process(message.asText());
 *     session.sendText(reply);
 *
 *     // Or async for long operations
 *     session.sendTextAsync(() -> expensiveOperation());
 * }
 * }</pre>
 *
 * @see WebSocketHandler
 * @see WebSocketSessionManager
 */
public interface WebSocketSession {

    /**
     * Returns the unique session ID.
     *
     * @return the session identifier
     */
    String id();

    /**
     * Closes the session with specific code and reason.
     *
     * @param code   the close status code
     * @param reason the close reason
     */
    void close(int code, @NotNull String reason);

    /**
     * Returns true if the session is still open.
     *
     * @return true if connected
     */
    boolean isOpen();

    /**
     * Returns path variables from the WebSocket path.
     *
     * @return map of variable name to value
     */
    Map<String, String> pathVariables();

    /**
     * Returns request headers from the WebSocket handshake.
     *
     * @return map of header name to value
     */
    Map<String, String> headers();

    /** Sends text synchronously. */
    void sendText(String text);

    /** Sends binary data synchronously. */
    void sendBinary(byte[] data);

    /** Closes the session synchronously. */
    void close();

    /**
     * Sends text asynchronously.
     *
     * @param text The text to send
     * @return CompletableFuture that completes when to send is done
     */
    default CompletableFuture<Void> sendTextAsync(Supplier<String> text) {
        return CompletableFuture.supplyAsync(text).thenAccept(this::sendTextInIoThread);
    }

    /**
     * Sends binary data asynchronously.
     *
     * @param data The binary data to send
     * @return CompletableFuture that completes when to send is done
     */
    default CompletableFuture<Void> sendBinaryAsync(Supplier<byte[]> data) {
        return CompletableFuture.supplyAsync(data).thenAccept(this::sendBinaryInIoThread);
    }

    /**
     * Sends text in an IO thread for non-blocking http server implementations.
     *
     * @param text The text to send
     * @return CompletableFuture that completes when to send is done
     */
    void sendTextInIoThread(String text);

    /**
     * Sends binary data in an IO thread for non-blocking http server implementations.
     *
     * @param data The binary data to send
     * @return CompletableFuture that completes when the send is done
     */
    void sendBinaryInIoThread(byte[] data);

    /**
     * Closes the session asynchronously.
     *
     * @return CompletableFuture that completes when the close is done
     */
    default CompletableFuture<Void> closeAsync() {
        return CompletableFuture.runAsync(this::close);
    }
}