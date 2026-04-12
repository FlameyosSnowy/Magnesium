package net.magnesiumbackend.transport.undertow.websocket;

import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedBinaryMessage;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.StreamSourceFrameChannel;
import io.undertow.websockets.core.WebSocketChannel;
import net.magnesiumbackend.core.http.websocket.DefaultWebSocketMessage;
import net.magnesiumbackend.core.http.websocket.WebSocketHandlerWrapper;
import net.magnesiumbackend.core.http.websocket.WebSocketSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.Pooled;

import java.nio.ByteBuffer;

public class UndertowWebSocketListener extends AbstractReceiveListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(UndertowWebSocketListener.class);

    private final WebSocketHandlerWrapper handler;
    private final WebSocketSessionManager sessionManager;
    private final UndertowWebSocketSession session;
    private final String path;

    public UndertowWebSocketListener(
        WebSocketHandlerWrapper handler,
        WebSocketSessionManager sessionManager,
        UndertowWebSocketSession session,
        String path
    ) {
        this.handler        = handler;
        this.sessionManager = sessionManager;
        this.session        = session;
        this.path           = path;
    }

    @Override
    protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) {
        try {
            handler.onMessage(session, DefaultWebSocketMessage.ofText(message.getData())).join();
        } catch (Exception e) {
            LOGGER.error("WebSocket onMessage error", e);
        }
    }

    @Override
    protected void onFullBinaryMessage(WebSocketChannel channel, BufferedBinaryMessage message) {
        try {
            Pooled<ByteBuffer[]> pooled = message.getData();
            ByteBuffer[] buffers = pooled.getResource();
            int total = 0;
            for (ByteBuffer b : buffers) total += b.remaining();
            byte[] bytes = new byte[total];
            int offset = 0;
            for (ByteBuffer b : buffers) {
                int len = b.remaining();
                b.get(bytes, offset, len);
                offset += len;
            }
            pooled.close();
            handler.onMessage(session, DefaultWebSocketMessage.ofBinary(bytes)).join();
        } catch (Exception e) {
            LOGGER.error("WebSocket onMessage error", e);
        }
    }

    @Override
    protected void onClose(WebSocketChannel webSocketChannel, StreamSourceFrameChannel channel) {
        sessionManager.remove(path, session);
        try {
            handler.onClose(session, 1000, "Connection closed").join();
        } catch (Exception e) {
            LOGGER.error("WebSocket onClose error", e);
        }
    }

    @Override
    protected void onError(WebSocketChannel channel, Throwable error) {
        LOGGER.error("WebSocket channel error", error);
        try {
            handler.onError(session, error).join();
        } catch (Exception e) {
            LOGGER.error("WebSocket onError handler threw", e);
        }
    }
}