package net.magnesiumbackend.transport.netty.websocket;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import net.magnesiumbackend.core.http.websocket.WebSocketSession;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public class NettyWebSocketSession implements WebSocketSession {
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyWebSocketSession.class);

    private final String id;
    private final Channel channel;
    private final Map<String, String> pathVariables;
    private final Map<String, String> headers;
    private final ChannelHandlerContext ctx;

    public NettyWebSocketSession(Channel channel, Map<String, String> pathVariables, Map<String, String> headers, ChannelHandlerContext ctx) {
        this.ctx = ctx;
        this.id            = UUID.randomUUID().toString();
        this.channel       = channel;
        this.pathVariables = Collections.unmodifiableMap(pathVariables);
        this.headers       = Collections.unmodifiableMap(headers);
    }

    @Override public String id()                        { return id; }

    @Override
    public void close(int code, @NotNull String reason) {
        try {
            channel.close().sync().await();
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted while closing channel", e);
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override public Map<String, String> pathVariables() { return pathVariables; }
    @Override public Map<String, String> headers()       { return headers; }

    @Override
    public void sendText(String text) {
        if (channel.isActive()) channel.writeAndFlush(new TextWebSocketFrame(text));
    }

    @Override
    public void sendBinary(byte[] data) {
        if (channel.isActive()) channel.writeAndFlush(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(data)));
    }

    @Override
    public void close() {
        if (channel.isActive())
            channel.writeAndFlush(new CloseWebSocketFrame()).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void sendTextInIoThread(String text) {
        ctx.executor().execute(() -> sendText(text));
    }

    @Override
    public void sendBinaryInIoThread(byte[] data) {
        ctx.executor().execute(() -> sendBinary(data));
    }

    public Channel channel() { return channel; }
}