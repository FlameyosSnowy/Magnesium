package net.magnesiumbackend.transport.netty.handler;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import net.magnesiumbackend.core.http.DefaultRequest;
import net.magnesiumbackend.core.http.HttpMethod;
import net.magnesiumbackend.core.http.HttpResponseWriter;
import net.magnesiumbackend.core.http.HttpUtils;
import net.magnesiumbackend.core.http.messages.MessageConverterRegistry;
import net.magnesiumbackend.core.http.Request;
import net.magnesiumbackend.core.http.ResponseEntity;
import net.magnesiumbackend.core.http.websocket.WebSocketRouteRegistry;
import net.magnesiumbackend.core.http.websocket.WebSocketSessionManager;
import net.magnesiumbackend.core.exceptions.ExceptionHandlerRegistry;
import net.magnesiumbackend.core.route.HttpRouteRegistry;
import net.magnesiumbackend.core.route.HttpFilter;
import net.magnesiumbackend.core.route.RequestContext;
import net.magnesiumbackend.core.route.RouteDefinition;
import net.magnesiumbackend.core.route.RouteTree;
import net.magnesiumbackend.core.security.CorrelationIdFilter;
import net.magnesiumbackend.core.security.SecurityHeadersFilter;
import net.magnesiumbackend.core.security.SslConfig;
import net.magnesiumbackend.core.headers.HttpHeaderIndex;
import net.magnesiumbackend.core.headers.Slice;
import net.magnesiumbackend.transport.netty.adapter.NettyHeaderAdapter;
import net.magnesiumbackend.transport.netty.adapter.NettyResponseAdapter;
import net.magnesiumbackend.transport.netty.websocket.NettyWebSocketServerHandler;
import net.magnesiumbackend.transport.netty.websocket.NettyWebSocketSession;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static net.magnesiumbackend.transport.netty.NettyToMagnesiumBridge.asMagnesiumMethod;
import static net.magnesiumbackend.transport.netty.NettyToMagnesiumBridge.asMagnesiumVersion;

public class NettyHttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final byte[] NOT_FOUND = "Not Found".getBytes(StandardCharsets.UTF_8);

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyHttpServerHandler.class);
    private static final String WEB_SOCKET_SECURED = "wss";
    private static final String WEB_SOCKET_UNSECURED = "ws";

    private final HttpRouteRegistry httpRouteRegistry;
    private final List<HttpFilter> globalFilters;
    private final ExceptionHandlerRegistry exceptionHandlerRegistry;
    private final MessageConverterRegistry messageConverterRegistry;
    private final WebSocketRouteRegistry webSocketRouteRegistry;
    private final WebSocketSessionManager sessionManager;
    @Nullable
    private final SslConfig sslConfig;
    private final boolean securedWebSocket;
    private final SecurityHeadersFilter securityHeadersFilter;

    public NettyHttpServerHandler(
        HttpRouteRegistry httpRouteRegistry,
        List<HttpFilter> globalFilters,
        ExceptionHandlerRegistry exceptionHandlerRegistry,
        MessageConverterRegistry messageConverterRegistry,
        WebSocketRouteRegistry webSocketRouteRegistry,
        WebSocketSessionManager sessionManager,
        @Nullable
        SslConfig sslConfig,
        SecurityHeadersFilter securityHeadersFilter
    ) {
        this.httpRouteRegistry = httpRouteRegistry;
        this.globalFilters = globalFilters;
        this.exceptionHandlerRegistry = exceptionHandlerRegistry;
        this.messageConverterRegistry = messageConverterRegistry;
        this.webSocketRouteRegistry = webSocketRouteRegistry;
        this.sessionManager = sessionManager;
        this.sslConfig = sslConfig;
        this.securedWebSocket = sslConfig != null && sslConfig.isWebSocketSecured();
        this.securityHeadersFilter = securityHeadersFilter;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, @NotNull FullHttpRequest nettyReq) {
        if (isWebSocketUpgrade(nettyReq)) {
            handleWebSocketUpgrade(ctx, nettyReq);
            return;
        }

        HttpMethod httpMethod = asMagnesiumMethod(nettyReq.method());

        String uri = nettyReq.uri();
        int qIndex = uri.indexOf('?');
        String rawPath = qIndex > 0 ? uri.substring(0, qIndex) : uri;
        String queryString = qIndex > 0 ? uri.substring(qIndex + 1) : "";

        Optional<RouteTree.RouteMatch<RouteDefinition>> matchedRoute =
            httpRouteRegistry.find(httpMethod, rawPath);

        FullHttpResponse nettyResp = matchedRoute
            .map(matched -> {
                String content = nettyReq.content().toString(StandardCharsets.UTF_8);

                Map<String, String> queryParams = HttpUtils.parseQueryString(queryString);

                RouteDefinition definition = matched.handler();
                HttpHeaderIndex headerIndex =
                    NettyHeaderAdapter.from(nettyReq.headers());
                Request request = new DefaultRequest(
                    definition.path(),
                    content,
                    asMagnesiumVersion(nettyReq.protocolVersion()),
                    httpMethod,
                    queryParams,
                    matched.pathVariables(),
                    definition,
                    headerIndex
                );

                RequestContext ctxObj = new RequestContext(request);
                ResponseEntity<?> responseEntity =
                    definition.execute(ctxObj, this.globalFilters, exceptionHandlerRegistry);

                if (this.securityHeadersFilter != null) this.securityHeadersFilter.applyTo(responseEntity);

                HttpResponseWriter writer = new HttpResponseWriter(this.messageConverterRegistry);
                DefaultFullHttpResponse resp = new DefaultFullHttpResponse(
                    nettyReq.protocolVersion(),
                    HttpResponseStatus.valueOf(responseEntity.statusCode())
                );
                try {
                    Slice correlationId = request.header(CorrelationIdFilter.HEADER);

                    if (correlationId != null && correlationId.len() > 0) {
                        resp.headers().set(
                            CorrelationIdFilter.HEADER,
                            correlationId.materialize()
                        );
                    }

                    writer.write(responseEntity, new NettyResponseAdapter(resp));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }


                return resp;
            })
            .orElseGet(() -> new DefaultFullHttpResponse(
                nettyReq.protocolVersion(),
                HttpResponseStatus.NOT_FOUND,
                Unpooled.copiedBuffer(NOT_FOUND)
            ));

        nettyResp.headers().setInt(
            HttpHeaderNames.CONTENT_LENGTH,
            nettyResp.content().readableBytes()
        );

        if (HttpUtil.isKeepAlive(nettyReq)) {
            nettyResp.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            ctx.writeAndFlush(nettyResp);
        } else {
            ctx.writeAndFlush(nettyResp).addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(@NotNull ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error(cause.getMessage(), cause);
        ctx.close();
    }

    private static boolean isWebSocketUpgrade(FullHttpRequest req) {
        return "websocket".equalsIgnoreCase(req.headers().get(HttpHeaderNames.UPGRADE));
    }

    private void handleWebSocketUpgrade(ChannelHandlerContext ctx, FullHttpRequest req) {
        String uri     = req.uri();
        int qIndex     = uri.indexOf('?');
        String rawPath = qIndex > 0 ? uri.substring(0, qIndex) : uri;

        var match = webSocketRouteRegistry.match(rawPath);
        if (match.isEmpty()) {
            DefaultFullHttpResponse resp = new DefaultFullHttpResponse(
                req.protocolVersion(),
                HttpResponseStatus.NOT_FOUND,
                Unpooled.copiedBuffer("No WebSocket route: " + rawPath, StandardCharsets.UTF_8)
            );
            ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
            return;
        }

        var matched = match.get();
        HttpHeaders nettyHttpHeaders = req.headers();
        String scheme = securedWebSocket ? WEB_SOCKET_SECURED : WEB_SOCKET_UNSECURED;
        String wsUrl  = scheme + "://" + req.headers().get(HttpHeaderNames.HOST) + rawPath;

        WebSocketServerHandshakerFactory factory =
            new WebSocketServerHandshakerFactory(wsUrl, null, true);

        WebSocketServerHandshaker handshaker = factory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
            return;
        }

        Map<String, String> headers = new HashMap<>(nettyHttpHeaders.size());
        nettyHttpHeaders.iteratorAsString().forEachRemaining(e -> headers.put(e.getKey(), e.getValue()));

        NettyWebSocketSession session = new NettyWebSocketSession(
            ctx.channel(), matched.pathVariables(), headers
        );

        handshaker.handshake(ctx.channel(), req).addListener(future -> {
            if (!future.isSuccess()) {
                ctx.fireExceptionCaught(future.cause());
                return;
            }
            ctx.pipeline().replace(
                this,
                "websocket-handler",
                new NettyWebSocketServerHandler(
                    matched.handler(), handshaker, session, sessionManager, rawPath
                )
            );
        });
    }
}