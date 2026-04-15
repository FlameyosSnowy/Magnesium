package net.magnesiumbackend.transport.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.http2.*;
import net.magnesiumbackend.core.backpressure.QueueRejectedError;
import net.magnesiumbackend.core.cancellation.SimpleCancellationToken;
import net.magnesiumbackend.core.exceptions.ExceptionHandlerRegistry;
import net.magnesiumbackend.core.headers.HttpHeaderIndex;
import net.magnesiumbackend.core.headers.HttpQueryParamIndex;
import net.magnesiumbackend.core.headers.Slice;
import net.magnesiumbackend.core.http.*;
import net.magnesiumbackend.core.http.messages.MessageConverterRegistry;
import net.magnesiumbackend.core.http.response.*;
import net.magnesiumbackend.core.route.*;
import net.magnesiumbackend.core.security.SecurityHeadersFilter;
import net.magnesiumbackend.transport.netty.adapter.NettyHeaderAdapter;
import net.magnesiumbackend.transport.netty.utils.NettySlices;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static net.magnesiumbackend.transport.netty.NettyToMagnesiumBridge.*;

public class Http2ServerHandler extends SimpleChannelInboundHandler<Http2StreamFrame> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Http2ServerHandler.class);
    private static final byte[] EMPTY = new byte[0];
    private static final byte[] NOT_FOUND_BYTES = "Not Found".getBytes(StandardCharsets.UTF_8);
    private static final byte[] SERVER_ERROR_BYTES = "Internal Server Error".getBytes(StandardCharsets.UTF_8);
    private static final byte[] OVERLOADED_BYTES = "Overloaded".getBytes(StandardCharsets.UTF_8);
    private static final byte[] TIMEOUT_BYTES = "Timeout".getBytes(StandardCharsets.UTF_8);

    private final HttpRouteRegistry routes;
    private final List<HttpFilter> filters;
    private final ExceptionHandlerRegistry exceptions;
    private final MessageConverterRegistry converters;
    @Nullable private final SecurityHeadersFilter security;
    @Nullable private final Executor executor;
    private final Duration timeout;

    private Http2HeadersFrame headersFrame;

    // IMPORTANT: keep as ByteBuf, not builder
    private ByteBuf body;

    public Http2ServerHandler(
        HttpRouteRegistry routes,
        List<HttpFilter> filters,
        ExceptionHandlerRegistry exceptions,
        MessageConverterRegistry converters,
        @Nullable SecurityHeadersFilter security,
        @Nullable Executor executor,
        Duration timeout
    ) {
        this.routes = routes;
        this.filters = filters;
        this.exceptions = exceptions;
        this.converters = converters;
        this.security = security;
        this.executor = executor;
        this.timeout = timeout;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http2StreamFrame frame) {

        if (frame instanceof Http2HeadersFrame hf) {
            headersFrame = hf;

            if (hf.isEndStream()) {
                handle(ctx, hf, EMPTY);
            }
            return;
        }

        if (frame instanceof Http2DataFrame df) {

            ByteBuf content = df.content();
            if (body == null) {
                body = ctx.alloc().buffer(content.readableBytes());
            }
            body.writeBytes(content);

            if (df.isEndStream()) {
                byte[] bytes = new byte[body.readableBytes()];
                body.readBytes(bytes);
                body.release();
                body = null;

                handle(ctx, headersFrame, bytes);
                headersFrame = null;
            }

            df.release();
        }
    }

    private void handle(ChannelHandlerContext ctx, Http2HeadersFrame frame, byte[] body) {

        Http2Headers http2Headers = frame.headers();

        Slice pathSlice = NettySlices.of(http2Headers.path());

        int qIndex = indexOf(pathSlice, (byte) '?');
        Slice path = qIndex >= 0
            ? new Slice(pathSlice.src(), pathSlice.start(), qIndex)
            : pathSlice;

        HttpMethod method = asMagnesiumMethod(http2Headers.method());

        RouteTree.RouteMatch<RouteDefinition> match =
            routes.find(method, path);

        if (match == null) {
            sendError(ctx, 404, NOT_FOUND_BYTES);
            return;
        }

        RouteDefinition def = match.handler();

        HttpHeaderIndex headers = NettyHeaderAdapter.from(http2Headers, ctx.alloc());

        HttpQueryParamIndex query = HttpUtils.parseQueryString(pathSlice.src());

        Request request = new DefaultRequest(
            def.path(),
            body,
            HttpVersion.HTTP_2_0,
            method,
            query,
            match.pathVariables(),
            def,
            headers
        );

        RequestContext context = new RequestContext(request);

        SimpleCancellationToken token = new SimpleCancellationToken();
        ctx.channel().closeFuture().addListener(f -> token.cancel());
        context.setCancellationToken(token);

        try {
            def.executeAsync(context, filters, exceptions, executor)
                .orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .whenComplete((res, err) -> {

                    token.cancel();

                    if (err != null) {
                        if (err instanceof java.util.concurrent.TimeoutException) {
                            sendError(ctx, 408, TIMEOUT_BYTES);
                        } else {
                            LOGGER.error("HTTP/2 failure", err);
                            sendError(ctx, 500, SERVER_ERROR_BYTES);
                        }
                        return;
                    }

                    if (security != null) {
                        security.applyTo(res);
                    }

                    sendResponse(ctx, res, request);
                });

        } catch (QueueRejectedError rejected) {
            sendError(ctx, 503, OVERLOADED_BYTES);
        }
    }

    private int indexOf(Slice s, byte b) {
        byte[] data = s.src();
        int start = s.start();
        int end = start + s.length();

        for (int i = start; i < end; i++) {
            if (data[i] == b) return i - start;
        }
        return -1;
    }

    private void sendError(ChannelHandlerContext ctx, int status, byte[] body) {
        Http2Headers headers = new DefaultHttp2Headers()
            .status(String.valueOf(status))
            .setInt("content-length", body.length);

        ctx.write(new DefaultHttp2HeadersFrame(headers, false));

        ByteBuf buf = ctx.alloc().buffer(body.length);
        buf.writeBytes(body);

        ctx.writeAndFlush(new DefaultHttp2DataFrame(buf, true));
    }

    private void sendResponse(ChannelHandlerContext ctx, ResponseEntity<?> entity, Request req) {

        try {
            Http2Headers headers = new DefaultHttp2Headers()
                .status(String.valueOf(entity.statusCode()));

            Slice corr = req.header("X-Correlation-Id");
            if (corr != null && corr.len() > 0) {
                headers.set("x-correlation-id", corr.materialize());
            }

            HttpHeaderIndex headerIndex = entity.headers();
            HttpHeaderIndex.Iterator iterator = headerIndex.iterator();
            while (iterator.next()) {
                headers.set(iterator.nameSlice(), iterator.valueSlice());
            }

            byte[] body = new HttpResponseWriter(converters).toBytes(entity);
            headers.setInt("content-length", body.length);

            ctx.write(new DefaultHttp2HeadersFrame(headers, false));

            ByteBuf buf = ctx.alloc().buffer(body.length);
            buf.writeBytes(body);

            ctx.writeAndFlush(new DefaultHttp2DataFrame(buf, true));

        } catch (Exception e) {
            LOGGER.error("response failure", e);
            sendError(ctx, 500, SERVER_ERROR_BYTES);
        }
    }
}