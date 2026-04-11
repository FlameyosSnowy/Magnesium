package net.magnesiumbackend.core.http.websocket;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class WebSocketRouteBuilder {

    private Consumer<WebSocketSession> onOpen = s -> {};
    private BiConsumer<WebSocketSession, WebSocketMessage> onMessage = (s, m) -> {};
    private OnClose onClose = (s, c, r) -> {};
    private BiConsumer<WebSocketSession, Throwable> onError = (s, e) -> {};

    public WebSocketRouteBuilder onOpen(Consumer<WebSocketSession> fn) {
        this.onOpen = fn;
        return this;
    }

    public WebSocketRouteBuilder onMessage(BiConsumer<WebSocketSession, WebSocketMessage> fn) {
        this.onMessage = fn;
        return this;
    }

    public WebSocketRouteBuilder onClose(OnClose fn) {
        this.onClose = fn;
        return this;
    }

    public WebSocketRouteBuilder onError(BiConsumer<WebSocketSession, Throwable> fn) {
        this.onError = fn;
        return this;
    }

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

    @FunctionalInterface
    public interface OnClose {
        void accept(WebSocketSession session, int code, String reason);
    }
}