package net.magnesiumbackend.transport.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import net.magnesiumbackend.core.http.socket.DefaultWebSocketMessage;
import net.magnesiumbackend.core.http.socket.WebSocketHandler;
import net.magnesiumbackend.core.http.socket.WebSocketSessionManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class NettyWebSocketServerHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyWebSocketServerHandler.class);

    private final WebSocketHandler handler;
    private final WebSocketServerHandshaker handshaker;
    private final NettyWebSocketSession session;
    private final WebSocketSessionManager sessionManager;
    private final String path;

    public NettyWebSocketServerHandler(
        WebSocketHandler handler,
        WebSocketServerHandshaker handshaker,
        NettyWebSocketSession session,
        WebSocketSessionManager sessionManager,
        String path
    ) {
        this.handler        = handler;
        this.handshaker     = handshaker;
        this.session        = session;
        this.sessionManager = sessionManager;
        this.path           = path;
    }

    @Override
    public void channelActive(@NotNull ChannelHandlerContext ctx) {
        sessionManager.add(path, session);
        try {
            handler.onOpen(session);
        } catch (Exception e) {
            LOGGER.error("WebSocket onOpen error", e);
            ctx.close();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (frame instanceof CloseWebSocketFrame close) {
            handshaker.close(ctx.channel(), close.retain());
            return;
        }
        if (frame instanceof PingWebSocketFrame) {
            ctx.writeAndFlush(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        if (frame instanceof PongWebSocketFrame) {
            return; // heartbeat reply, nothing to do
        }

        try {
            if (frame instanceof TextWebSocketFrame text) {
                handler.onMessage(session, DefaultWebSocketMessage.ofText(text.text()));
            } else if (frame instanceof BinaryWebSocketFrame binary) {
                byte[] bytes = new byte[binary.content().readableBytes()];
                binary.content().readBytes(bytes);
                handler.onMessage(session, DefaultWebSocketMessage.ofBinary(bytes));
            } else if (frame instanceof ContinuationWebSocketFrame cont) {
                // surface raw bytes; reassembly is the handler's responsibility if needed
                byte[] bytes = new byte[cont.content().readableBytes()];
                cont.content().readBytes(bytes);
                handler.onMessage(session, DefaultWebSocketMessage.ofBinary(bytes));
            }
        } catch (Exception e) {
            LOGGER.error("WebSocket onMessage error", e);
        }
    }

    @Override
    public void channelInactive(@NotNull ChannelHandlerContext ctx) {
        sessionManager.remove(path, session);
        try {
            handler.onClose(session, 1001, "Connection closed");
        } catch (Exception e) {
            LOGGER.error("WebSocket onClose error", e);
        }
    }

    @Override
    public void exceptionCaught(@NotNull ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("WebSocket channel error", cause);
        try {
            handler.onError(session, cause);
        } catch (Exception e) {
            LOGGER.error("WebSocket onError handler threw", e);
        }
        ctx.close();
    }
}