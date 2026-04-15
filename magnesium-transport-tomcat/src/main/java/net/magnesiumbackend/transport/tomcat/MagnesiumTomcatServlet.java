package net.magnesiumbackend.transport.tomcat;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.magnesiumbackend.core.http.DefaultRequest;
import net.magnesiumbackend.core.http.response.HttpMethod;
import net.magnesiumbackend.core.http.response.HttpResponseWriter;
import net.magnesiumbackend.core.http.response.HttpUtils;
import net.magnesiumbackend.core.http.response.HttpVersion;
import net.magnesiumbackend.core.http.messages.MessageConverterRegistry;
import net.magnesiumbackend.core.cancellation.SimpleCancellationToken;
import net.magnesiumbackend.core.exceptions.ExceptionHandlerRegistry;
import net.magnesiumbackend.core.route.HttpRouteRegistry;
import net.magnesiumbackend.core.route.HttpFilter;
import net.magnesiumbackend.core.route.RequestContext;
import net.magnesiumbackend.core.route.RouteDefinition;
import net.magnesiumbackend.core.route.RouteTree;
import net.magnesiumbackend.core.security.SecurityHeadersFilter;
import net.magnesiumbackend.core.headers.HttpHeaderIndex;
import net.magnesiumbackend.transport.tomcat.adapter.TomcatHeaderAdapter;
import net.magnesiumbackend.transport.tomcat.adapter.TomcatResponseAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

public class MagnesiumTomcatServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(MagnesiumTomcatServlet.class);
    private static final byte[] INTERNAL_SERVER_ERROR = "Internal Server Error".getBytes(StandardCharsets.UTF_8);
    private static final byte[] EMPTY_BYTES = new byte[0];
    private static final byte[] NOT_FOUND = "Not Found".getBytes(StandardCharsets.UTF_8);

    private final HttpRouteRegistry httpRouteRegistry;
    private final List<HttpFilter> globalFilters;
    private final ExceptionHandlerRegistry exceptionHandlerRegistry;
    private final MessageConverterRegistry messageConverterRegistry;
    private final SecurityHeadersFilter securityHeadersFilter;
    private final Executor executor;
    private final Duration defaultTimeout;

    public MagnesiumTomcatServlet(
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
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        HttpMethod method = HttpMethod.valueOf(req.getMethod());
        String path = req.getRequestURI();

        HttpVersion version = "HTTP/1.0".equals(req.getProtocol())
            ? HttpVersion.HTTP_1_0
            : HttpVersion.HTTP_1_1;

        RouteTree.RouteMatch<RouteDefinition> matchedRoute =
            httpRouteRegistry.find(method, path);

        if (matchedRoute == null) {
            resp.setStatus(404);
            resp.setContentType("application/json");
            resp.getOutputStream().write(NOT_FOUND);
            return;
        }

        RouteDefinition definition = matchedRoute.handler();
        byte[] body = readBody(req);
        String query = req.getQueryString();

        HttpHeaderIndex headerIndex = TomcatHeaderAdapter.from(req);

        DefaultRequest request = new DefaultRequest(
            definition.path(),
            body,
            version,
            method,
            HttpUtils.parseQueryString(query),
            matchedRoute.pathVariables(),
            definition,
            headerIndex
        );

        RequestContext ctxObj = new RequestContext(request);
        ctxObj.setTimeout(this.defaultTimeout);

        // Create cancellation token for this request
        SimpleCancellationToken cancellationToken = new SimpleCancellationToken();
        ctxObj.setCancellationToken(cancellationToken);

        // Note: Tomcat doesn't provide easy async cancellation hooks
        // The token is mainly for handler code to check ctx.cancellationToken().isCancelled()

        definition.executeAsync(
                ctxObj,
                globalFilters,
                exceptionHandlerRegistry,
                executor,
                ctxObj.timeout()
            )
            .whenComplete((responseEntity, throwable) -> {

                if (throwable != null) {
                    LOGGER.error("Error processing request", throwable);
                    try {
                        resp.setStatus(500);
                        resp.getOutputStream().write(INTERNAL_SERVER_ERROR);
                    } catch (IOException ignored) {}
                    return;
                }

                try {
                    if (securityHeadersFilter != null) {
                        securityHeadersFilter.applyTo(responseEntity);
                    }

                    HttpResponseWriter writer =
                        new HttpResponseWriter(messageConverterRegistry);

                    writer.write(responseEntity, new TomcatResponseAdapter(resp));

                } catch (Exception e) {
                    LOGGER.error("Error writing response", e);
                    try {
                        resp.setStatus(500);
                        resp.getOutputStream().write(
                            "Internal Server Error".getBytes(StandardCharsets.UTF_8)
                        );
                    } catch (IOException ignored) {}
                }
            });
    }

    private byte[] readBody(HttpServletRequest req) throws IOException {
        String contentLengthHeader = req.getHeader("Content-Length");
        try (var is = req.getInputStream()) {
            if (contentLengthHeader != null) {
                int contentLength = Integer.parseInt(contentLengthHeader);
                if (contentLength == 0) return EMPTY_BYTES;
                return is.readNBytes(contentLength);
            }
            return is.readAllBytes();
        }
    }
}