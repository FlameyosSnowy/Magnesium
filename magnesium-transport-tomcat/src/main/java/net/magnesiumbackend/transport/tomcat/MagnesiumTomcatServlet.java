package net.magnesiumbackend.transport.tomcat;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.magnesiumbackend.core.http.DefaultRequest;
import net.magnesiumbackend.core.http.HttpMethod;
import net.magnesiumbackend.core.http.HttpResponseWriter;
import net.magnesiumbackend.core.http.HttpUtils;
import net.magnesiumbackend.core.http.HttpVersion;
import net.magnesiumbackend.core.http.messages.MessageConverterRegistry;
import net.magnesiumbackend.core.http.ResponseEntity;
import net.magnesiumbackend.core.registry.ExceptionHandlerRegistry;
import net.magnesiumbackend.core.registry.HttpRouteRegistry;
import net.magnesiumbackend.core.route.HttpFilter;
import net.magnesiumbackend.core.route.RequestContext;
import net.magnesiumbackend.core.route.RouteDefinition;
import net.magnesiumbackend.core.route.RouteTree;
import net.magnesiumbackend.core.security.SecurityHeadersFilter;
import net.magnesiumbackend.core.headers.HttpHeaderIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MagnesiumTomcatServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(MagnesiumTomcatServlet.class);
    private static final byte[] INTERNAL_SERVER_ERROR = "Internal Server Error".getBytes(StandardCharsets.UTF_8);

    private final HttpRouteRegistry httpRouteRegistry;
    private final List<HttpFilter> globalFilters;
    private final ExceptionHandlerRegistry exceptionHandlerRegistry;
    private final MessageConverterRegistry messageConverterRegistry;
    private final SecurityHeadersFilter securityHeadersFilter;

    public MagnesiumTomcatServlet(HttpRouteRegistry httpRouteRegistry, List<HttpFilter> globalFilters, ExceptionHandlerRegistry exceptionHandlerRegistry, MessageConverterRegistry messageConverterRegistry, SecurityHeadersFilter securityHeadersFilter) {
        this.httpRouteRegistry = httpRouteRegistry;
        this.globalFilters = globalFilters;
        this.exceptionHandlerRegistry = exceptionHandlerRegistry;
        this.messageConverterRegistry = messageConverterRegistry;
        this.securityHeadersFilter = securityHeadersFilter;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            HttpMethod method = HttpMethod.valueOf(req.getMethod());
            String path = req.getRequestURI();

            HttpVersion version = "HTTP/1.0".equals(req.getProtocol())
                ? HttpVersion.HTTP_1_0
                : HttpVersion.HTTP_1_1;

            Enumeration<String> headerNames = req.getHeaderNames();
            Map<String, String> requestHeaders = new HashMap<>();
            for (Iterator<String> it = headerNames.asIterator(); it.hasNext(); ) {
                String name = it.next();
                requestHeaders.put(name, req.getHeader(name));
            }

            Optional<RouteTree.RouteMatch<RouteDefinition>> matchedRoute = httpRouteRegistry.find(method, path);

            if (matchedRoute.isEmpty()) {
                resp.setStatus(404);
                resp.setContentType("application/json");
                resp.getOutputStream().write("Not Found".getBytes(StandardCharsets.UTF_8));
                return;
            }

            RouteTree.RouteMatch<RouteDefinition> matched = matchedRoute.get();
            RouteDefinition definition = matched.handler();

            String body = readBody(req);

            String query = req.getQueryString(); // HttpServletRequest already splits this

            HttpHeaderIndex headerIndex =
                TomcatHeaderAdapter.from(req);

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
            httpResponseWriter.write(responseEntity, new TomcatResponseAdapter(resp));
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            resp.setStatus(500);
            resp.getOutputStream().write(INTERNAL_SERVER_ERROR);
        }
    }

    private String readBody(HttpServletRequest req) throws IOException {
        String contentLengthHeader = req.getHeader("Content-Length");
        try (var is = req.getInputStream()) {
            if (contentLengthHeader != null) {
                int contentLength = Integer.parseInt(contentLengthHeader);
                if (contentLength == 0) return "";
                return new String(is.readNBytes(contentLength), StandardCharsets.UTF_8);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}