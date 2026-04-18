package net.magnesiumbackend.transport.httpserver;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;
import net.magnesiumbackend.core.MagnesiumRuntime;
import net.magnesiumbackend.core.MagnesiumRuntime;
import net.magnesiumbackend.core.backpressure.BackpressureExecutorResolver;
import net.magnesiumbackend.core.headers.HttpPathParamIndex;
import net.magnesiumbackend.core.http.MagnesiumTransport;
import net.magnesiumbackend.core.http.websocket.WebSocketHandlerWrapper;
import net.magnesiumbackend.core.http.websocket.WebSocketRouteRegistry;
import net.magnesiumbackend.core.http.websocket.WebSocketSessionManager;
import net.magnesiumbackend.core.route.HttpRouteRegistry;
import net.magnesiumbackend.core.route.RoutePathTemplate;
import net.magnesiumbackend.core.route.RouteTree;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import net.magnesiumbackend.core.security.SslConfig;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class HttpServerMagnesiumTransport implements MagnesiumTransport {
    private HttpServer server;

    @Override
    public void bind(int port, MagnesiumRuntime runtime, HttpRouteRegistry routes) {
        SslConfig sslConfig = runtime.sslConfig();

        try {
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
        } catch (BindException e) {
            throw new RuntimeException(
                "Port " + port + " is already in use. Is another instance of the server running?", e);
        } catch (IOException e) {
            throw new RuntimeException(
                "Failed to bind " + (sslConfig != null ? "HTTPS" : "HTTP") +
                    " server on port " + port + ": " + e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(
                "SSL algorithm '" + e.getMessage() + "' is not supported by this JVM. " +
                    "Check your SslConfig cipher suite and protocol configuration.", e);
        } catch (KeyStoreException e) {
            throw new RuntimeException(
                "Failed to load the keystore. Verify the keystore path, format (JKS/PKCS12), " +
                    "and that the file is not corrupted: " + e.getMessage(), e);
        } catch (UnrecoverableKeyException e) {
            throw new RuntimeException(
                "Could not recover the private key from the keystore. " +
                    "The key password is likely incorrect.", e);
        } catch (KeyManagementException e) {
            throw new RuntimeException(
                "Failed to initialise the SSL context. This usually means the configured " +
                    "certificates and keys are inconsistent or incompatible: " + e.getMessage(), e);
        }

        Executor executor = BackpressureExecutorResolver.resolve(runtime);
        server.setExecutor(Objects.requireNonNullElseGet(executor, Executors::newVirtualThreadPerTaskExecutor));

        server.createContext("/", new MagnesiumHttpHandler(
            routes,
            runtime.router().globalFilters(),
            runtime.exceptionHandlerRegistry(),
            runtime.messageConverterRegistry(),
            runtime.securityHeadersFilter(),
            executor,
            runtime.defaultTimeout()
        ));

        registerWebSocketRoutes(runtime);

        try {
            runtime.application().start(runtime);
        } catch (Exception e) {
            runtime.application().startFuture().completeExceptionally(e);
            throw new IllegalStateException("Application failed during start()", e);
        }

        server.start();

        // Wait for application to signal it's ready before accepting connections
        try {
            runtime.application().startFuture().get();
        } catch (Exception e) {
            server.stop(0);
            throw new RuntimeException("Application startFuture failed", e);
        }

        try {
            runtime.application().ready(runtime, getPort());
        } catch (Exception e) {
            throw new RuntimeException("[Magnesium] Undertow transport interrupted by an error from Application#ready.", e);
        }

        try {
            runtime.shutdownLatch().await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void registerWebSocketRoutes(MagnesiumRuntime application) {
        WebSocketRouteRegistry wsRegistry = application.router().webSocketRouteRegistry();
        WebSocketSessionManager sessionManager = application.router().webSocketSessionManager();

        for (RouteTree.RouteEntry<WebSocketHandlerWrapper> entry : wsRegistry.entries()) {

            String path = entry.path();
            String contextPath = toContextPath(path);

            server.createContext(contextPath, exchange -> {

                String requestPath = exchange.getRequestURI().getRawPath();

                RouteTree.RouteMatch<WebSocketHandlerWrapper> match = wsRegistry
                    .tree()
                    .match(requestPath.getBytes(StandardCharsets.UTF_8));
                HttpPathParamIndex pathVars = match == null ? HttpPathParamIndex.empty() : match.pathVariables();

                HttpServerWebSocketHandler webSocketHandler =
                    new HttpServerWebSocketHandler(
                        entry.handler(),
                        sessionManager,
                        path,
                        pathVars
                    );

                webSocketHandler.handle(exchange);
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