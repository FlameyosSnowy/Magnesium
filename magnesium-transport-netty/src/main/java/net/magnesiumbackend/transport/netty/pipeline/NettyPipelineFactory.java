package net.magnesiumbackend.transport.netty.pipeline;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http2.*;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.ssl.SslContext;
import net.magnesiumbackend.core.http.messages.MessageConverterRegistry;
import net.magnesiumbackend.core.http.websocket.WebSocketRouteRegistry;
import net.magnesiumbackend.core.http.websocket.WebSocketSessionManager;
import net.magnesiumbackend.core.exceptions.ExceptionHandlerRegistry;
import net.magnesiumbackend.core.route.HttpRouteRegistry;
import net.magnesiumbackend.core.route.HttpFilter;
import net.magnesiumbackend.core.security.SecurityHeadersFilter;
import net.magnesiumbackend.core.security.SslConfig;
import net.magnesiumbackend.transport.netty.handler.Http2ServerHandler;
import net.magnesiumbackend.transport.netty.handler.NettyHttpServerHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Central pipeline factory — all pipeline construction lives here.
 * Both the ChannelInitializer and the ALPN negotiator delegate to this
 * class, eliminating duplication between HTTP/1.1 and HTTP/2 paths.
 */
public final class NettyPipelineFactory {

    /** Max aggregated request body size — 10 MB default. */
    private static final int DEFAULT_MAX_CONTENT_LENGTH = 10 * 1024 * 1024;

    private final HttpRouteRegistry         httpRouteRegistry;
    private final List<HttpFilter>          globalFilters;
    private final ExceptionHandlerRegistry  exceptionHandlerRegistry;
    private final MessageConverterRegistry  messageConverterRegistry;
    private final WebSocketRouteRegistry    webSocketRouteRegistry;
    private final WebSocketSessionManager   sessionManager;
    private final SecurityHeadersFilter     securityHeadersFilter;
    private final int                       maxContentLength;

    @Nullable
    private final SslConfig sslConfig;

    public NettyPipelineFactory(
        HttpRouteRegistry        httpRouteRegistry,
        List<HttpFilter>         globalFilters,
        ExceptionHandlerRegistry exceptionHandlerRegistry,
        MessageConverterRegistry messageConverterRegistry,
        WebSocketRouteRegistry   webSocketRouteRegistry,
        WebSocketSessionManager  sessionManager,
        @Nullable SslConfig      sslConfig,
        SecurityHeadersFilter    securityHeadersFilter
    ) {
        this(
            httpRouteRegistry, globalFilters, exceptionHandlerRegistry,
            messageConverterRegistry, webSocketRouteRegistry, sessionManager,
            sslConfig, securityHeadersFilter, DEFAULT_MAX_CONTENT_LENGTH
        );
    }

    public NettyPipelineFactory(
        HttpRouteRegistry        httpRouteRegistry,
        List<HttpFilter>         globalFilters,
        ExceptionHandlerRegistry exceptionHandlerRegistry,
        MessageConverterRegistry messageConverterRegistry,
        WebSocketRouteRegistry   webSocketRouteRegistry,
        WebSocketSessionManager  sessionManager,
        @Nullable SslConfig      sslConfig,
        SecurityHeadersFilter    securityHeadersFilter,
        int                      maxContentLength
    ) {
        this.httpRouteRegistry        = httpRouteRegistry;
        this.globalFilters            = globalFilters;
        this.exceptionHandlerRegistry = exceptionHandlerRegistry;
        this.messageConverterRegistry = messageConverterRegistry;
        this.webSocketRouteRegistry   = webSocketRouteRegistry;
        this.sessionManager           = sessionManager;
        this.sslConfig                = sslConfig;
        this.securityHeadersFilter    = securityHeadersFilter;
        this.maxContentLength         = maxContentLength;
    }

    /**
     * Configures the full pipeline for an incoming channel.
     * If SSL is present, adds the SslHandler + ALPN negotiator.
     * If not, falls directly through to HTTP/1.1.
     */
    public void initChannel(ChannelPipeline pipeline, @Nullable SslContext nettyCtx) {
        if (nettyCtx != null && sslConfig != null) {
            pipeline.addLast(nettyCtx.newHandler(pipeline.channel().alloc()));
            pipeline.addLast(buildAlpnNegotiator());
        } else {
            configureHttp11(pipeline);
        }
    }

    /** HTTP/1.1: codec → aggregator → Magnesium handler */
    public void configureHttp11(ChannelPipeline pipeline) {
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(maxContentLength));
        pipeline.addLast(buildHttp11Handler());
    }

    /** HTTP/2: frame codec → multiplexer → Magnesium handler per stream */
    public void configureHttp2(ChannelPipeline pipeline) {
        pipeline.addLast(Http2FrameCodecBuilder.forServer().build());
        pipeline.addLast(new Http2MultiplexHandler(buildHttp2Handler()));
    }

    private NettyHttpServerHandler buildHttp11Handler() {
        return new NettyHttpServerHandler(
            httpRouteRegistry,
            globalFilters,
            exceptionHandlerRegistry,
            messageConverterRegistry,
            webSocketRouteRegistry,
            sessionManager,
            sslConfig,
            securityHeadersFilter
        );
    }

    private Http2ServerHandler buildHttp2Handler() {
        return new Http2ServerHandler(
            httpRouteRegistry,
            globalFilters,
            exceptionHandlerRegistry,
            messageConverterRegistry,
            securityHeadersFilter
        );
    }

    private ApplicationProtocolNegotiationHandler buildAlpnNegotiator() {
        NettyPipelineFactory factory = this;

        return new ApplicationProtocolNegotiationHandler(ApplicationProtocolNames.HTTP_1_1) {
            @Override
            protected void configurePipeline(io.netty.channel.ChannelHandlerContext ctx,
                                             String protocol) {
                switch (protocol) {
                    case ApplicationProtocolNames.HTTP_2    -> factory.configureHttp2(ctx.pipeline());
                    case ApplicationProtocolNames.HTTP_1_1  -> factory.configureHttp11(ctx.pipeline());
                    default -> throw new IllegalStateException(
                        "[Magnesium] Unknown ALPN protocol: " + protocol
                    );
                }
            }
        };
    }
}
