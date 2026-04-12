package net.magnesiumbackend.core.http.websocket;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface WebSocketSession {

    String id();

    void close(int code, @NotNull String reason);

    boolean isOpen();

    Map<String, String> pathVariables();

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