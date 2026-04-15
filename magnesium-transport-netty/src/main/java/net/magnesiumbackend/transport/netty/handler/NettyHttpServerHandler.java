package net.magnesiumbackend.transport.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import net.magnesiumbackend.core.backpressure.QueueRejectedError;
import net.magnesiumbackend.core.cancellation.SimpleCancellationToken;
import net.magnesiumbackend.core.exceptions.ExceptionHandlerRegistry;
import net.magnesiumbackend.core.headers.HttpHeaderIndex;
import net.magnesiumbackend.core.headers.HttpQueryParamIndex;
import net.magnesiumbackend.core.headers.Slice;
import net.magnesiumbackend.core.http.DefaultRequest;
import net.magnesiumbackend.core.http.Request;
import net.magnesiumbackend.core.http.messages.MessageConverterRegistry;
import net.magnesiumbackend.core.http.response.*;
import net.magnesiumbackend.core.http.response.HttpMethod;
import net.magnesiumbackend.core.route.*;
import net.magnesiumbackend.core.security.SecurityHeadersFilter;
import net.magnesiumbackend.transport.netty.adapter.NettyHeaderAdapter;
import net.magnesiumbackend.transport.netty.adapter.NettyResponseAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static net.magnesiumbackend.transport.netty.NettyToMagnesiumBridge.*;

public class NettyHttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyHttpServerHandler.class);
    private static final byte[] NOT_FOUND = "Not Found".getBytes(StandardCharsets.UTF_8);

    private final HttpRouteRegistry httpRouteRegistry;
    private final List<HttpFilter> globalFilters;
    private final ExceptionHandlerRegistry exceptionHandlerRegistry;
    private final MessageConverterRegistry messageConverterRegistry;
    private final SecurityHeadersFilter securityHeadersFilter;
    @Nullable private final Executor requestExecutor;
    private final Duration timeout;

    public NettyHttpServerHandler(
        HttpRouteRegistry httpRouteRegistry,
        List<HttpFilter> globalFilters,
        ExceptionHandlerRegistry exceptionHandlerRegistry,
        MessageConverterRegistry messageConverterRegistry,
        @Nullable SecurityHeadersFilter securityHeadersFilter,
        @Nullable Executor requestExecutor,
        Duration timeout
    ) {
        this.httpRouteRegistry = httpRouteRegistry;
        this.globalFilters = globalFilters;
        this.exceptionHandlerRegistry = exceptionHandlerRegistry;
        this.messageConverterRegistry = messageConverterRegistry;
        this.securityHeadersFilter = securityHeadersFilter;
        this.requestExecutor = requestExecutor;
        this.timeout = timeout;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, @NotNull FullHttpRequest req) {
        HttpMethod method = asMagnesiumMethod(req.method());

        String uri = req.uri();
        int qIndex = uri.indexOf('?');

        String path = qIndex > 0 ? uri.substring(0, qIndex) : uri;

        RouteTree.RouteMatch<RouteDefinition> matched =
            httpRouteRegistry.find(method, path);

        if (matched == null) {
            writeNotFound(ctx, req);
            return;
        }

        RouteDefinition def = matched.handler();

        ByteBuf content = req.content();
        byte[] body = ByteBufUtil.getBytes(content);
        HttpQueryParamIndex queryParams = HttpUtils.parseQueryString(req.uri());

        HttpHeaderIndex headers = NettyHeaderAdapter.from(req.headers(), ctx.alloc());
        Request request = new DefaultRequest(
            def.path(),
            body,
            asMagnesiumVersion(req.protocolVersion()),
            method,
            queryParams,
            matched.pathVariables(),
            def,
            headers
        );

        RequestContext context = new RequestContext(request);

        execute(ctx, req, context, def);
    }

    private void execute(
        ChannelHandlerContext ctx,
        FullHttpRequest req,
        RequestContext context,
        RouteDefinition def
    ) {
        SimpleCancellationToken token = new SimpleCancellationToken();
        ctx.channel().closeFuture().addListener(_ -> token.cancel());
        context.setCancellationToken(token);

        CompletableFuture<ResponseEntity<?>> future;

        try {
            future = def.executeAsync(context, globalFilters, exceptionHandlerRegistry, requestExecutor)
                .orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (QueueRejectedError rejected) {
            writeRejected(ctx, req);
            return;
        }

        future.whenComplete((res, err) -> {
            token.cancel();

            if (err != null) {
                if (err instanceof java.util.concurrent.TimeoutException) {
                    writeTimeout(ctx, req);
                } else {
                    writeError(ctx, req, err);
                }
                return;
            }

            writeSuccess(ctx, req, context, res);
        });
    }

    private void writeSuccess(
        ChannelHandlerContext ctx,
        FullHttpRequest req,
        RequestContext context,
        ResponseEntity<?> entity
    ) {
        try {
            if (securityHeadersFilter != null) {
                securityHeadersFilter.applyTo(entity);
            }

            DefaultFullHttpResponse resp = new DefaultFullHttpResponse(
                req.protocolVersion(),
                HttpResponseStatus.valueOf(entity.statusCode())
            );

            Slice correlation = context.request().header("X-Correlation-Id");
            if (correlation != null && correlation.len() > 0) {
                resp.headers().set("X-Correlation-Id", correlation.materialize());
            }

            HttpResponseWriter writer = new HttpResponseWriter(messageConverterRegistry);
            writer.write(entity, new NettyResponseAdapter(resp));

            write(ctx, req, resp);

        } catch (IOException e) {
            writeError(ctx, req, e);
        }
    }

    private void writeError(ChannelHandlerContext ctx, FullHttpRequest req, Throwable t) {
        LOGGER.error("Request failed", t);

        FullHttpResponse resp = new DefaultFullHttpResponse(
            req.protocolVersion(),
            HttpResponseStatus.INTERNAL_SERVER_ERROR,
            Unpooled.copiedBuffer("Internal Server Error", StandardCharsets.UTF_8)
        );

        write(ctx, req, resp);
    }

    private void writeTimeout(ChannelHandlerContext ctx, FullHttpRequest req) {
        FullHttpResponse resp = new DefaultFullHttpResponse(
            req.protocolVersion(),
            HttpResponseStatus.REQUEST_TIMEOUT,
            Unpooled.copiedBuffer("Request Timeout", StandardCharsets.UTF_8)
        );

        write(ctx, req, resp);
    }

    private void writeRejected(ChannelHandlerContext ctx, FullHttpRequest req) {
        FullHttpResponse resp = new DefaultFullHttpResponse(
            req.protocolVersion(),
            HttpResponseStatus.SERVICE_UNAVAILABLE,
            Unpooled.copiedBuffer("Server Overloaded", StandardCharsets.UTF_8)
        );

        write(ctx, req, resp);
    }

    private void write(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse resp) {
        resp.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, resp.content().readableBytes());

        if (ctx.executor().inEventLoop()) {
            writeDirect(ctx, req, resp);
        } else {
            ctx.executor().execute(() -> writeDirect(ctx, req, resp));
        }
    }

    private void writeDirect(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse resp) {
        if (HttpUtil.isKeepAlive(req)) {
            ctx.writeAndFlush(resp);
        } else {
            ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
        }
    }

    private void writeNotFound(ChannelHandlerContext ctx, FullHttpRequest req) {
        FullHttpResponse resp = new DefaultFullHttpResponse(
            req.protocolVersion(),
            HttpResponseStatus.NOT_FOUND,
            Unpooled.copiedBuffer(NOT_FOUND)
        );

        resp.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, NOT_FOUND.length);
        write(ctx, req, resp);
    }

    @Override
    public void exceptionCaught(@NotNull ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("Unhandled pipeline error", cause);
        ctx.close();
    }
}