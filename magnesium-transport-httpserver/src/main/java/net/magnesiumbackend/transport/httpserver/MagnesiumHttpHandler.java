package net.magnesiumbackend.transport.httpserver;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.magnesiumbackend.core.headers.HttpQueryParamIndex;
import net.magnesiumbackend.core.http.DefaultRequest;
import net.magnesiumbackend.core.http.response.HttpMethod;
import net.magnesiumbackend.core.http.response.HttpResponseWriter;
import net.magnesiumbackend.core.http.response.HttpUtils;
import net.magnesiumbackend.core.http.response.HttpVersion;
import net.magnesiumbackend.core.http.messages.MessageConverterRegistry;
import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.cancellation.SimpleCancellationToken;
import net.magnesiumbackend.core.exceptions.ExceptionHandlerRegistry;
import net.magnesiumbackend.core.route.HttpRouteRegistry;
import net.magnesiumbackend.core.route.HttpFilter;
import net.magnesiumbackend.core.route.RequestContext;
import net.magnesiumbackend.core.route.RouteDefinition;
import net.magnesiumbackend.core.route.RouteTree;
import net.magnesiumbackend.core.security.SecurityHeadersFilter;
import net.magnesiumbackend.core.headers.HttpHeaderIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

public class MagnesiumHttpHandler implements HttpHandler {
    private static final byte[] NOT_FOUND = "Not Found".getBytes(StandardCharsets.UTF_8);
    private static final Logger LOGGER = LoggerFactory.getLogger(MagnesiumHttpHandler.class);

    private final HttpRouteRegistry httpRouteRegistry;
    private final List<HttpFilter> globalFilters;
    private final ExceptionHandlerRegistry exceptionHandlerRegistry;
    private final MessageConverterRegistry messageConverterRegistry;
    private final SecurityHeadersFilter securityHeadersFilter;
    private final Executor executor;
    private final Duration defaultTimeout;

    public MagnesiumHttpHandler(
        HttpRouteRegistry httpRouteRegistry,
        List<HttpFilter> globalFilters,
        ExceptionHandlerRegistry exceptionHandlerRegistry,
        MessageConverterRegistry messageConverterRegistry,
        SecurityHeadersFilter securityHeadersFilter,
        Executor executor,
        Duration defaultTimeout
    ) {
        this.httpRouteRegistry = httpRouteRegistry;
        this.globalFilters = globalFilters;
        this.exceptionHandlerRegistry = exceptionHandlerRegistry;
        this.messageConverterRegistry = messageConverterRegistry;
        this.securityHeadersFilter = securityHeadersFilter;
        this.executor = executor;
        this.defaultTimeout = defaultTimeout != null ? defaultTimeout : Duration.ofSeconds(30);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            HttpVersion version = switch (exchange.getProtocol()) {
                case "HTTP/1.0" -> HttpVersion.HTTP_1_0;
                case "HTTP/2"   -> HttpVersion.HTTP_2_0;
                default         -> HttpVersion.HTTP_1_1;
            };

            HttpMethod method = HttpMethod.valueOf(exchange.getRequestMethod());
            String uri = exchange.getRequestURI().getRawPath();
            String queryString = exchange.getRequestURI().getRawQuery(); // null if absent

            Optional<RouteTree.RouteMatch<RouteDefinition>> matchedRoute =
                httpRouteRegistry.find(method, uri);

            if (matchedRoute.isEmpty()) {
                exchange.sendResponseHeaders(404, NOT_FOUND.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(NOT_FOUND); }
                return;
            }

            RouteTree.RouteMatch<RouteDefinition> matched = matchedRoute.get();
            RouteDefinition definition = matched.handler();

            Headers requestHeaders = exchange.getRequestHeaders();

            HttpHeaderIndex headerIndex =
                JdkHeaderAdapter.from(requestHeaders);
            HttpQueryParamIndex queryParams = HttpUtils.parseQueryString(queryString);

            byte[] body;
            String contentLengthHeader = requestHeaders.getFirst("Content-Length");
            if (contentLengthHeader != null) {
                int len = Integer.parseInt(contentLengthHeader);
                body = len == 0 ? new byte[0] : exchange.getRequestBody().readNBytes(len);
            } else {
                body = exchange.getRequestBody().readAllBytes();
            }

            DefaultRequest request = new DefaultRequest(
                definition.path(),
                body,
                version,
                method,
                queryParams,
                matched.pathVariables(),
                definition,
                headerIndex
            );

            RequestContext ctxObj = new RequestContext(request);
            ctxObj.setTimeout(this.defaultTimeout);

            // Create cancellation token for this request
            SimpleCancellationToken cancellationToken = new SimpleCancellationToken();
            ctxObj.setCancellationToken(cancellationToken);

            // Note: JDK HttpServer doesn't provide async cancellation hooks
            // The token is mainly for handler code to check ctx.cancellationToken().isCancelled()

            ResponseEntity<?> responseEntity =
                definition.execute(ctxObj, globalFilters, exceptionHandlerRegistry);

            if (this.securityHeadersFilter != null) this.securityHeadersFilter.applyTo(responseEntity);

            HttpResponseWriter writer = new HttpResponseWriter(this.messageConverterRegistry);
            writer.write(responseEntity, new JdkHttpResponseAdapter(exchange));

        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            exchange.sendResponseHeaders(500, 0);
        } finally {
            exchange.getResponseBody().close();
        }
    }
}