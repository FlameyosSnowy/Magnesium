package net.magnesiumbackend.transport.netty.pipeline;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.ssl.SslContext;
import net.magnesiumbackend.core.MagnesiumApplication;
import net.magnesiumbackend.core.http.messages.MessageConverterRegistry;
import net.magnesiumbackend.core.http.websocket.WebSocketRouteRegistry;
import net.magnesiumbackend.core.exceptions.ExceptionHandlerRegistry;
import net.magnesiumbackend.core.route.HttpRouteRegistry;
import net.magnesiumbackend.core.security.SslConfig;
import net.magnesiumbackend.transport.netty.adapter.NettySslAdapter;
import net.magnesiumbackend.transport.netty.handler.NettyHttpServerHandler;
import org.jetbrains.annotations.Nullable;

public class NettySocketChannelInitializer extends ChannelInitializer<SocketChannel> {
    private final HttpRouteRegistry httpRouteRegistry;
    private final WebSocketRouteRegistry webSocketRouteRegistry;
    private final ExceptionHandlerRegistry exceptionHandlerRegistry;
    private final MessageConverterRegistry messageConverterRegistry;
    private final MagnesiumApplication application;
    private final @Nullable SslConfig sslConfig;

    public NettySocketChannelInitializer(HttpRouteRegistry httpRouteRegistry, WebSocketRouteRegistry webSocketRouteRegistry, ExceptionHandlerRegistry exceptionHandlerRegistry, MessageConverterRegistry messageConverterRegistry, MagnesiumApplication application, @Nullable SslConfig sslConfig) {
        this.httpRouteRegistry = httpRouteRegistry;
        this.webSocketRouteRegistry = webSocketRouteRegistry;
        this.exceptionHandlerRegistry = exceptionHandlerRegistry;
        this.messageConverterRegistry = messageConverterRegistry;
        this.application = application;
        this.sslConfig = sslConfig;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();

        if (sslConfig != null) {
            try {
                SslContext nettyCtx = NettySslAdapter.toNettyContext(sslConfig);
                pipeline.addLast(nettyCtx.newHandler(ch.alloc()));
            } catch (Exception e) {
                throw new IllegalStateException("[Magnesium] Failed to initialize Netty SSL context.", e);
            }
        }

        pipeline.addLast(new HttpRequestDecoder());
        pipeline.addLast(new HttpResponseEncoder());
        pipeline.addLast(new HttpObjectAggregator(65536));
        pipeline.addLast(new WebSocketServerCompressionHandler(0));
        pipeline.addLast(new NettyHttpServerHandler(
            httpRouteRegistry,
            application.httpServer().globalFilters(),
            application.exceptionHandlerRegistry(),
            application.messageConverterRegistry(),
            application.httpServer().webSocketRouteRegistry(),
            application.httpServer().webSocketSessionManager(),
            sslConfig,
            application.securityHeadersFilter()
        ));
    }
}