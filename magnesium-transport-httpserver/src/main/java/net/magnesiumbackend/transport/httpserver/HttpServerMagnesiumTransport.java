package net.magnesiumbackend.transport.httpserver;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;
import net.magnesiumbackend.core.MagnesiumApplication;
import net.magnesiumbackend.core.http.MagnesiumTransport;
import net.magnesiumbackend.core.http.websocket.WebSocketHandler;
import net.magnesiumbackend.core.http.websocket.WebSocketRouteRegistry;
import net.magnesiumbackend.core.http.websocket.WebSocketSessionManager;
import net.magnesiumbackend.core.route.HttpRouteRegistry;
import net.magnesiumbackend.core.route.RoutePathTemplate;
import net.magnesiumbackend.core.route.RouteTree;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import net.magnesiumbackend.core.security.SslConfig;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.Executors;

public class HttpServerMagnesiumTransport implements MagnesiumTransport {
    private HttpServer server;

    @Override
    public void bind(int port, MagnesiumApplication application, HttpRouteRegistry routes) {
        try {
            SslConfig sslConfig = application.sslConfig();

            if (sslConfig != null) {
                HttpsServer httpsServer = HttpsServer.create(new InetSocketAddress(port), 0);
                httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslConfig.sslContext()) {
                    @Override
                    public void configure(HttpsParameters params) {
                        params.setSSLParameters(getSSLContext().getDefaultSSLParameters());
                    }
                });
                server = httpsServer;
            } else {
                server = HttpServer.create(new InetSocketAddress(port), 0);
            }

            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

            server.createContext("/", new MagnesiumHttpHandler(
                routes,
                application.httpServer().globalFilters(),
                application.exceptionHandlerRegistry(),
                application.messageConverterRegistry(),
                application.securityHeadersFilter()
            ));

            registerWebSocketRoutes(application);

            server.start();
            application.onStart().accept(application.serviceRegistry());
            application.shutdownLatch().await();
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private void registerWebSocketRoutes(MagnesiumApplication application) {
        WebSocketRouteRegistry wsRegistry = application.httpServer().webSocketRouteRegistry();
        WebSocketSessionManager sessionManager = application.httpServer().webSocketSessionManager();

        for (RouteTree.RouteEntry<WebSocketHandler> entry : wsRegistry.entries()) {
            String path        = entry.path();
            String contextPath = toContextPath(path);

            server.createContext(contextPath, exchange -> {
                System.out.println("[DEBUG] WebSocket request: path=" + path + ", contextPath=" + contextPath + ", requestURI=" + exchange.getRequestURI().getPath());
                String requestPath = exchange.getRequestURI().getPath();
                RoutePathTemplate template = RoutePathTemplate.compile(path);
                System.out.println("[DEBUG] Template: " + java.util.Arrays.toString(template.literals()) + ", " + java.util.Arrays.toString(template.varNames()));
                Map<String, String> pathVars = template.match(requestPath);
                System.out.println("[DEBUG] Match result: " + pathVars);
                if (pathVars == null) {
                    pathVars = Map.of();
                }

                new HttpServerWebSocketHandler(
                    entry.handler(),
                    sessionManager,
                    path,
                    pathVars
                ).handle(exchange);
            });
        }
    }

    private String toContextPath(String path) {
        int braceIndex = path.indexOf('{');
        if (braceIndex < 0) return path;
        int lastSlash = path.lastIndexOf('/', braceIndex);
        return lastSlash <= 0 ? "/" : path.substring(0, lastSlash + 1);
    }

    @Override
    public void shutdown() {
        if (server != null) server.stop(0);
    }

    @Override
    public int getPort() {
        if (server == null) {
            throw new IllegalStateException("Server not started yet");
        }
        return server.getAddress().getPort();
    }
}