package net.magnesiumbackend.transport.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http2.*;
import net.magnesiumbackend.core.backpressure.QueueRejectedError;
import net.magnesiumbackend.core.cancellation.SimpleCancellationToken;
import net.magnesiumbackend.core.exceptions.ExceptionHandlerRegistry;
import net.magnesiumbackend.core.headers.HttpHeaderIndex;
import net.magnesiumbackend.core.headers.Slice;
import net.magnesiumbackend.core.http.DefaultRequest;
import net.magnesiumbackend.core.http.Request;
import net.magnesiumbackend.core.http.messages.MessageConverterRegistry;
import net.magnesiumbackend.core.http.response.*;
import net.magnesiumbackend.core.route.*;
import net.magnesiumbackend.core.security.SecurityHeadersFilter;
import net.magnesiumbackend.transport.netty.adapter.NettyHeaderAdapter;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static net.magnesiumbackend.transport.netty.NettyToMagnesiumBridge.*;

public class Http2ServerHandler extends SimpleChannelInboundHandler<Http2StreamFrame> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Http2ServerHandler.class);

    private final HttpRouteRegistry httpRouteRegistry;
    private final List<HttpFilter> globalFilters;
    private final ExceptionHandlerRegistry exceptionHandlerRegistry;
    private final MessageConverterRegistry messageConverterRegistry;
    @Nullable private final SecurityHeadersFilter securityHeadersFilter;
    @Nullable private final Executor requestExecutor;
    private final Duration timeout;

    private Http2HeadersFrame headersFrame;
    private final StringBuilder bodyBuffer = new StringBuilder();

    public Http2ServerHandler(
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
    protected void channelRead0(ChannelHandlerContext ctx, Http2StreamFrame frame) {
        if (frame instanceof Http2HeadersFrame headers) {
            headersFrame = headers;
            if (headers.isEndStream()) {
                handle(ctx, headers, "");
            }
            return;
        }

        if (frame instanceof Http2DataFrame data) {
            bodyBuffer.append(data.content().toString(StandardCharsets.UTF_8));
            data.release();

            if (data.isEndStream() && headersFrame != null) {
                handle(ctx, headersFrame, bodyBuffer.toString());
                bodyBuffer.setLength(0);
                headersFrame = null;
            }
        }
    }

    private void handle(ChannelHandlerContext ctx, Http2HeadersFrame headersFrame, String body) {
        Http2Headers h2Headers = headersFrame.headers();

        String rawPath = h2Headers.path().toString();
        int qIndex = rawPath.indexOf('?');

        String path = qIndex > 0 ? rawPath.substring(0, qIndex) : rawPath;
        String query = qIndex > 0 ? rawPath.substring(qIndex + 1) : "";

        HttpMethod method = asMagnesiumMethodFromString(h2Headers.method().toString());

        Optional<RouteTree.RouteMatch<RouteDefinition>> match =
            httpRouteRegistry.find(method, path);

        if (match.isEmpty()) {
            sendError(ctx, 404, "Not Found");
            return;
        }

        var matched = match.get();
        RouteDefinition def = matched.handler();

        HttpHeaderIndex headers = NettyHeaderAdapter.from(h2Headers);

        Request request = new DefaultRequest(
            def.path(),
            body,
            HttpVersion.HTTP_2_0,
            method,
            HttpUtils.parseQueryString(query),
            matched.pathVariables(),
            def,
            headers
        );

        RequestContext ctxObj = new RequestContext(request);

        SimpleCancellationToken token = new SimpleCancellationToken();
        ctx.channel().closeFuture().addListener(f -> token.cancel());
        ctxObj.setCancellationToken(token);

        try {
            def.executeAsync(ctxObj, globalFilters, exceptionHandlerRegistry, requestExecutor)
                .orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .whenComplete((res, err) -> {
                    token.cancel();

                    if (err != null) {
                        if (err instanceof java.util.concurrent.TimeoutException) {
                            sendError(ctx, 408, "Request Timeout");
                        } else {
                            LOGGER.error("HTTP/2 error", err);
                            sendError(ctx, 500, "Internal Server Error");
                        }
                        return;
                    }

                    if (securityHeadersFilter != null) {
                        securityHeadersFilter.applyTo(res);
                    }

                    sendResponse(ctx, res, request);
                });

        } catch (QueueRejectedError rejected) {
            sendError(ctx, 503, "Server Overloaded");
        }
    }

    private void sendResponse(ChannelHandlerContext ctx, ResponseEntity<?> entity, Request request) {
        try {
            Http2Headers headers = new DefaultHttp2Headers()
                .status(String.valueOf(entity.statusCode()));

            Slice correlation = request.header("X-Correlation-Id");
            if (correlation != null && correlation.len() > 0) {
                headers.set("x-correlation-id", correlation.materialize());
            }

            for (Map.Entry<String, String> e : entity.headers().entrySet()) {
                headers.set(e.getKey().toLowerCase(), e.getValue());
            }

            HttpResponseWriter writer = new HttpResponseWriter(messageConverterRegistry);
            byte[] body = writer.toBytes(entity);

            headers.setInt("content-length", body.length);

            ctx.write(new DefaultHttp2HeadersFrame(headers, false));

            ByteBuf buf = ctx.alloc().buffer(body.length).writeBytes(body);
            ctx.writeAndFlush(new DefaultHttp2DataFrame(buf, true));

        } catch (Exception e) {
            LOGGER.error("HTTP/2 write failed", e);
            sendError(ctx, 500, "Internal Server Error");
        }
    }

    private void sendError(ChannelHandlerContext ctx, int status, String msg) {
        byte[] body = msg.getBytes(StandardCharsets.UTF_8);

        Http2Headers headers = new DefaultHttp2Headers()
            .status(String.valueOf(status))
            .setInt("content-length", body.length)
            .set("content-type", "text/plain; charset=UTF-8");

        ctx.write(new DefaultHttp2HeadersFrame(headers, false));
        ByteBuf buf = ctx.alloc().buffer(body.length).writeBytes(body);
        ctx.writeAndFlush(new DefaultHttp2DataFrame(buf, true));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("HTTP/2 pipeline error", cause);
        ctx.close();
    }
}