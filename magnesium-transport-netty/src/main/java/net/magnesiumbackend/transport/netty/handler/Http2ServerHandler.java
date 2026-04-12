package net.magnesiumbackend.transport.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http2.*;
import net.magnesiumbackend.core.backpressure.QueueRejectedError;
import net.magnesiumbackend.core.backpressure.RejectionResponse;
import net.magnesiumbackend.core.cancellation.SimpleCancellationToken;
import net.magnesiumbackend.core.exceptions.ExceptionHandlerRegistry;
import net.magnesiumbackend.core.headers.HttpHeaderIndex;
import net.magnesiumbackend.core.headers.Slice;
import net.magnesiumbackend.core.http.DefaultRequest;
import net.magnesiumbackend.core.http.Request;
import net.magnesiumbackend.core.http.messages.MessageConverterRegistry;
import net.magnesiumbackend.core.http.response.HttpMethod;
import net.magnesiumbackend.core.http.response.HttpResponseWriter;
import net.magnesiumbackend.core.http.response.HttpUtils;
import net.magnesiumbackend.core.http.response.HttpVersion;
import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.route.HttpFilter;
import net.magnesiumbackend.core.route.HttpRouteRegistry;
import net.magnesiumbackend.core.route.RequestContext;
import net.magnesiumbackend.core.route.RouteDefinition;
import net.magnesiumbackend.core.route.RouteTree;
import net.magnesiumbackend.core.security.CorrelationIdFilter;
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

import static net.magnesiumbackend.transport.netty.NettyToMagnesiumBridge.asMagnesiumMethodFromString;

/**
 * Per-stream HTTP/2 handler.
 *
 * <p>Netty's {@link Http2MultiplexHandler} creates one child channel per stream and
 * installs a fresh instance of this handler in each child channel's pipeline.
 * Instance fields ({@link #headersFrame}, {@link #bodyBuffer}) are therefore
 * stream-scoped and require no synchronisation.
 *
 * <h2>Cancellation</h2>
 * A {@link SimpleCancellationToken} is wired to the child channel's {@code closeFuture()}.
 * That future fires on clean end-of-stream <em>and</em> on RST_STREAM — the multiplexer
 * closes the child channel in both cases — so no manual {@link Http2ResetFrame} handling
 * is needed.
 *
 * <h2>Backpressure</h2>
 * {@code executeAsync()} is called with the {@code requestExecutor} supplied by
 * {@link net.magnesiumbackend.transport.netty.pipeline.NettyPipelineFactory}. When that
 * executor is a {@link net.magnesiumbackend.core.backpressure.BoundedBackpressureExecutor}
 * and its queue is full, {@link QueueRejectedError} is thrown synchronously before any
 * worker thread is touched. The handler catches it and replies immediately on the event
 * loop thread.
 */
public class Http2ServerHandler extends SimpleChannelInboundHandler<Http2StreamFrame> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Http2ServerHandler.class);

    private final HttpRouteRegistry         httpRouteRegistry;
    private final List<HttpFilter>          globalFilters;
    private final ExceptionHandlerRegistry  exceptionHandlerRegistry;
    private final MessageConverterRegistry  messageConverterRegistry;
    @Nullable private final SecurityHeadersFilter securityHeadersFilter;
    @Nullable private final Executor        requestExecutor;
    private final Duration                  timeout;

    private Http2HeadersFrame  headersFrame;
    private final StringBuilder bodyBuffer = new StringBuilder();

    public Http2ServerHandler(
        HttpRouteRegistry        httpRouteRegistry,
        List<HttpFilter>         globalFilters,
        ExceptionHandlerRegistry exceptionHandlerRegistry,
        MessageConverterRegistry messageConverterRegistry,
        @Nullable SecurityHeadersFilter securityHeadersFilter,
        @Nullable Executor       requestExecutor,
        Duration                 timeout
    ) {
        this.httpRouteRegistry        = httpRouteRegistry;
        this.globalFilters            = globalFilters;
        this.exceptionHandlerRegistry = exceptionHandlerRegistry;
        this.messageConverterRegistry = messageConverterRegistry;
        this.securityHeadersFilter    = securityHeadersFilter;
        this.requestExecutor          = requestExecutor;
        this.timeout                  = timeout;
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
            data.content().release();

            if (data.isEndStream() && headersFrame != null) {
                handle(ctx, headersFrame, bodyBuffer.toString());
                bodyBuffer.setLength(0);
                headersFrame = null;
            }
        }
    }

    private void handle(ChannelHandlerContext ctx, Http2HeadersFrame headersFrame, String body) {
        Http2Headers h2Headers = headersFrame.headers();

        String rawPath     = h2Headers.path().toString();
        int    qIndex      = rawPath.indexOf('?');
        String path        = qIndex > 0 ? rawPath.substring(0, qIndex)  : rawPath;
        String queryString = qIndex > 0 ? rawPath.substring(qIndex + 1) : "";

        HttpMethod method;
        try {
            method = asMagnesiumMethodFromString(h2Headers.method().toString());
        } catch (Exception e) {
            sendError(ctx, 400, "Invalid HTTP method");
            return;
        }

        Optional<RouteTree.RouteMatch<RouteDefinition>> matchedRoute =
            httpRouteRegistry.find(method, path);

        if (matchedRoute.isEmpty()) {
            sendError(ctx, 404, "Not Found");
            return;
        }

        RouteTree.RouteMatch<RouteDefinition> matched    = matchedRoute.get();
        RouteDefinition                       definition = matched.handler();

        HttpHeaderIndex headerIndex = NettyHeaderAdapter.from(h2Headers);

        Request request = new DefaultRequest(
            definition.path(),
            body,
            HttpVersion.HTTP_2_0,
            method,
            HttpUtils.parseQueryString(queryString),
            matched.pathVariables(),
            definition,
            headerIndex
        );

        RequestContext ctxObj = new RequestContext(request);
        ctxObj.setTimeout(timeout);

        // The child channel's closeFuture fires on both clean end-of-stream and
        // RST_STREAM (the multiplexer closes the child channel in both cases).
        // This covers cancellation at the stream level without requiring explicit
        // Http2ResetFrame handling.
        SimpleCancellationToken token = new SimpleCancellationToken();
        ctx.channel().closeFuture().addListener(f -> token.cancel());
        ctxObj.setCancellationToken(token);

        // If the executor is a BoundedBackpressureExecutor and its queue is full,
        // QueueRejectedError is thrown synchronously here on the event loop thread
        // before any CompletableFuture is created.
        try {
            definition.executeAsync(ctxObj, globalFilters, exceptionHandlerRegistry, requestExecutor)
                .orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .whenComplete((responseEntity, throwable) -> {
                    token.cancel();

                    if (throwable != null) {
                        if (throwable instanceof java.util.concurrent.TimeoutException) {
                            ctx.executor().execute(() -> sendError(ctx, 408, "Request Timeout"));
                        } else {
                            LOGGER.error("Error processing HTTP/2 request", throwable);
                            ctx.executor().execute(() -> sendError(ctx, 500, "Internal Server Error"));
                        }
                        return;
                    }

                    if (securityHeadersFilter != null) {
                        securityHeadersFilter.applyTo(responseEntity);
                    }

                    ctx.executor().execute(() -> sendResponse(ctx, responseEntity, request));
                });

        } catch (QueueRejectedError rejected) {
            LOGGER.debug(
                "HTTP/2 request rejected — queue full (capacity={}, path={})",
                rejected.config().queueCapacity(),
                path
            );
            sendRejection(ctx, rejected.rejectionResponse());
        }
    }

    private void sendResponse(
        ChannelHandlerContext ctx,
        ResponseEntity<?>     responseEntity,
        Request               request
    ) {
        try {
            Http2Headers responseHeaders = new DefaultHttp2Headers()
                .status(String.valueOf(responseEntity.statusCode()));

            Slice correlationId = request.header(CorrelationIdFilter.HEADER);
            if (correlationId != null && correlationId.len() > 0) {
                responseHeaders.set(CorrelationIdFilter.HEADER, correlationId.materialize());
            }

            for (Map.Entry<String, String> entry : responseEntity.headers().entrySet()) {
                responseHeaders.set(entry.getKey().toLowerCase(), entry.getValue());
            }

            HttpResponseWriter writer    = new HttpResponseWriter(messageConverterRegistry);
            byte[]             bodyBytes = writer.toBytes(responseEntity);

            if (bodyBytes.length == 0) {
                ctx.writeAndFlush(new DefaultHttp2HeadersFrame(responseHeaders, true));
                return;
            }

            responseHeaders.setInt("content-length", bodyBytes.length);
            ctx.write(new DefaultHttp2HeadersFrame(responseHeaders, false));

            ByteBuf buf = ctx.alloc().buffer(bodyBytes.length).writeBytes(bodyBytes);
            ctx.writeAndFlush(new DefaultHttp2DataFrame(buf, true));

        } catch (Exception e) {
            LOGGER.error("HTTP/2 response write failed", e);
            sendError(ctx, 500, "Internal Server Error");
        }
    }

    /**
     * Sends the configured {@link RejectionResponse} as an HTTP/2 HEADERS frame.
     *
     * <p>{@code retry-after} is emitted as a lowercase trailer header, consistent with
     * the HTTP/2 requirement that all header names be lowercase (RFC 9113 §8.2).
     */
    private void sendRejection(ChannelHandlerContext ctx, RejectionResponse rejection) {
        Http2Headers headers = new DefaultHttp2Headers()
            .status(String.valueOf(rejection.statusCode()));

        long retryAfterSeconds = rejection.retryAfterSeconds();
        if (retryAfterSeconds > 0) {
            headers.set("retry-after", String.valueOf(retryAfterSeconds));
        }

        String body = rejection.body();
        if (body == null || body.isEmpty()) {
            ctx.writeAndFlush(new DefaultHttp2HeadersFrame(headers, true));
            return;
        }

        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        headers.setInt("content-length", bodyBytes.length);
        headers.set("content-type", "text/plain; charset=UTF-8");

        ctx.write(new DefaultHttp2HeadersFrame(headers, false));
        ByteBuf buf = ctx.alloc().buffer(bodyBytes.length).writeBytes(bodyBytes);
        ctx.writeAndFlush(new DefaultHttp2DataFrame(buf, true));
    }

    private void sendError(ChannelHandlerContext ctx, int status, String message) {
        byte[]       body    = message.getBytes(StandardCharsets.UTF_8);
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
        LOGGER.error("HTTP/2 channel error", cause);
        ctx.close();
    }
}