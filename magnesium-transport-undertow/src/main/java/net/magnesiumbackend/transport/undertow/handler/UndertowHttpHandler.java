package net.magnesiumbackend.transport.undertow.handler;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import net.magnesiumbackend.core.http.DefaultRequest;
import net.magnesiumbackend.core.http.response.HttpMethod;
import net.magnesiumbackend.core.http.response.HttpResponseWriter;
import net.magnesiumbackend.core.http.response.HttpUtils;
import net.magnesiumbackend.core.http.response.HttpVersion;
import net.magnesiumbackend.core.http.messages.MessageConverterRegistry;
import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.http.websocket.WebSocketRouteRegistry;
import net.magnesiumbackend.core.http.websocket.WebSocketSessionManager;
import net.magnesiumbackend.core.exceptions.ExceptionHandlerRegistry;
import net.magnesiumbackend.core.route.HttpRouteRegistry;
import net.magnesiumbackend.core.route.HttpFilter;
import net.magnesiumbackend.core.route.RequestContext;
import net.magnesiumbackend.core.route.RouteDefinition;
import net.magnesiumbackend.core.route.RouteTree;
import net.magnesiumbackend.core.security.SecurityHeadersFilter;
import net.magnesiumbackend.core.security.SslConfig;
import net.magnesiumbackend.core.headers.HttpHeaderIndex;
import net.magnesiumbackend.transport.undertow.adapter.UndertowHeaderAdapter;
import net.magnesiumbackend.transport.undertow.adapter.UndertowResponseAdapter;
import net.magnesiumbackend.transport.undertow.websocket.UndertowWebSocketCallback;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class UndertowHttpHandler implements HttpHandler {
    private static final String NOT_FOUND = "Not Found";
    private static final Logger LOGGER = LoggerFactory.getLogger(UndertowHttpHandler.class);

    private final HttpRouteRegistry httpRouteRegistry;
    private final List<HttpFilter> globalFilters;
    private final ExceptionHandlerRegistry exceptionHandlerRegistry;
    private final MessageConverterRegistry messageConverterRegistry;
    private final WebSocketRouteRegistry webSocketRouteRegistry;
    private final WebSocketSessionManager sessionManager;
    @Nullable
    private final SslConfig sslConfig;
    private final SecurityHeadersFilter securityHeadersFilter;

    public UndertowHttpHandler(
        HttpRouteRegistry httpRouteRegistry,
        List<HttpFilter> globalFilters,
        ExceptionHandlerRegistry exceptionHandlerRegistry,
        MessageConverterRegistry messageConverterRegistry,
        WebSocketRouteRegistry webSocketRouteRegistry,
        WebSocketSessionManager sessionManager,
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
        this.securityHeadersFilter = securityHeadersFilter;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        String upgradeHeader = exchange.getRequestHeaders().getFirst(Headers.UPGRADE);
        if ("websocket".equalsIgnoreCase(upgradeHeader)) {
            handleWebSocketUpgrade(exchange);
            return;
        }

        exchange.startBlocking();

        try {
            HttpMethod method = HttpMethod.valueOf(exchange.getRequestMethod().toString());
            String path = exchange.getRequestPath();
            HttpVersion version = exchange.isHttp11() ? HttpVersion.HTTP_1_1 : HttpVersion.HTTP_1_0;

            Map<String, String> requestHeaders = new HashMap<>();
            for (HeaderValues headerValues : exchange.getRequestHeaders()) {
                String headerName = headerValues.getHeaderName().toString();
                if (!headerValues.isEmpty()) {
                    requestHeaders.put(headerName, headerValues.getFirst());
                }
            }

            Optional<RouteTree.RouteMatch<RouteDefinition>> matchedRoute = httpRouteRegistry.find(method, path);

            if (matchedRoute.isEmpty()) {
                exchange.setStatusCode(404);
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                exchange.getResponseSender().send(NOT_FOUND);
                return;
            }

            RouteTree.RouteMatch<RouteDefinition> matched = matchedRoute.get();
            RouteDefinition definition = matched.handler();

            String body = readBody(exchange);

            String query = exchange.getQueryString();

            HttpHeaderIndex headerIndex =
                UndertowHeaderAdapter.from(exchange);

            DefaultRequest request = new DefaultRequest(
                definition.path(),
                body,
                version,
                method,
                HttpUtils.parseQueryString(query),
                matched.pathVariables(),
                definition,
                headerIndex
            );

            RequestContext ctxObj = new RequestContext(request);
            ResponseEntity<?> responseEntity = definition.execute(ctxObj, globalFilters, exceptionHandlerRegistry);

            if (this.securityHeadersFilter != null) this.securityHeadersFilter.applyTo(responseEntity);

            HttpResponseWriter httpResponseWriter = new HttpResponseWriter(this.messageConverterRegistry);
            httpResponseWriter.write(responseEntity, new UndertowResponseAdapter(exchange));
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            exchange.setStatusCode(500);
            exchange.getResponseSender().send("Internal Server Error");
        }
    }

    private String readBody(HttpServerExchange exchange) throws IOException {
        String contentLengthHeader = exchange.getRequestHeaders().getFirst(Headers.CONTENT_LENGTH);
        try (InputStream is = exchange.getInputStream()) {
            if (contentLengthHeader != null) {
                int contentLength = Integer.parseInt(contentLengthHeader);
                if (contentLength == 0) return "";
                return new String(is.readNBytes(contentLength), StandardCharsets.UTF_8);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void handleWebSocketUpgrade(HttpServerExchange exchange) {
        String path = exchange.getRequestPath();
        var match = webSocketRouteRegistry.match(path);

        if (match.isEmpty()) {
            exchange.setStatusCode(404);
            exchange.getResponseSender().send("No WebSocket route: " + path);
            return;
        }

        var matched = match.get();

        UndertowWebSocketCallback callback = new UndertowWebSocketCallback(
            matched.handler(),
            sessionManager,
            path,
            matched.pathVariables()
        );

        io.undertow.websockets.WebSocketProtocolHandshakeHandler wsHandler =
            new io.undertow.websockets.WebSocketProtocolHandshakeHandler(callback);

        try {
            wsHandler.handleRequest(exchange);
        } catch (Exception e) {
            LOGGER.error("WebSocket upgrade failed", e);
            exchange.setStatusCode(500);
        }
    }
}