package net.magnesiumbackend.transport.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http2.*;
import net.magnesiumbackend.core.headers.HttpHeaderIndex;
import net.magnesiumbackend.core.headers.Slice;
import net.magnesiumbackend.core.http.*;
import net.magnesiumbackend.core.http.messages.MessageConverterRegistry;
import net.magnesiumbackend.core.exceptions.ExceptionHandlerRegistry;
import net.magnesiumbackend.core.http.response.HttpMethod;
import net.magnesiumbackend.core.http.response.HttpResponseWriter;
import net.magnesiumbackend.core.http.response.HttpUtils;
import net.magnesiumbackend.core.http.response.HttpVersion;
import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.route.HttpRouteRegistry;
import net.magnesiumbackend.core.route.*;
import net.magnesiumbackend.core.security.CorrelationIdFilter;
import net.magnesiumbackend.core.security.SecurityHeadersFilter;
import net.magnesiumbackend.transport.netty.adapter.NettyHeaderAdapter;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static net.magnesiumbackend.transport.netty.NettyToMagnesiumBridge.asMagnesiumMethodFromString;

public class Http2ServerHandler extends SimpleChannelInboundHandler<Http2StreamFrame> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Http2ServerHandler.class);

    private final HttpRouteRegistry         httpRouteRegistry;
    private final List<HttpFilter>          globalFilters;
    private final ExceptionHandlerRegistry  exceptionHandlerRegistry;
    private final MessageConverterRegistry  messageConverterRegistry;
    private final SecurityHeadersFilter     securityHeadersFilter;

    // Per-stream accumulation — one handler instance per stream via Http2MultiplexHandler
    private Http2HeadersFrame headersFrame;
    private final StringBuilder bodyBuffer = new StringBuilder();

    public Http2ServerHandler(
        HttpRouteRegistry        httpRouteRegistry,
        List<HttpFilter>         globalFilters,
        ExceptionHandlerRegistry exceptionHandlerRegistry,
        MessageConverterRegistry messageConverterRegistry,
        @Nullable SecurityHeadersFilter securityHeadersFilter
    ) {
        this.httpRouteRegistry        = httpRouteRegistry;
        this.globalFilters            = globalFilters;
        this.exceptionHandlerRegistry = exceptionHandlerRegistry;
        this.messageConverterRegistry = messageConverterRegistry;
        this.securityHeadersFilter    = securityHeadersFilter;
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

        String rawPath    = h2Headers.path().toString();
        int    qIndex     = rawPath.indexOf('?');
        String path       = qIndex > 0 ? rawPath.substring(0, qIndex) : rawPath;
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

        RouteTree.RouteMatch<RouteDefinition> matched = matchedRoute.get();
        RouteDefinition definition = matched.handler();

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

        RequestContext ctxObj   = new RequestContext(request);
        ResponseEntity<?> responseEntity =
            definition.execute(ctxObj, globalFilters, exceptionHandlerRegistry);

        if (securityHeadersFilter != null) securityHeadersFilter.applyTo(responseEntity);

        sendResponse(ctx, responseEntity, request);
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
                String key = entry.getKey();
                String value = entry.getValue();
                responseHeaders.set(key, value);
            }

            HttpResponseWriter writer = new HttpResponseWriter(messageConverterRegistry);
            byte[] bodyBytes = writer.toBytes(responseEntity);

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

    private void sendError(ChannelHandlerContext ctx, int status, String message) {
        byte[] body = message.getBytes(StandardCharsets.UTF_8);
        Http2Headers headers = new DefaultHttp2Headers()
            .status(String.valueOf(status))
            .setInt("content-length", body.length);
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