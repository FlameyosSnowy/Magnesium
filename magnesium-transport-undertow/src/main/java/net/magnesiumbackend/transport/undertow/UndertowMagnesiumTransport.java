package net.magnesiumbackend.transport.undertow;

import io.undertow.Undertow;
import net.magnesiumbackend.core.MagnesiumRuntime;
import net.magnesiumbackend.core.backpressure.BackpressureExecutorResolver;
import net.magnesiumbackend.core.http.MagnesiumTransport;
import net.magnesiumbackend.core.route.HttpRouteRegistry;
import net.magnesiumbackend.core.security.SslConfig;
import net.magnesiumbackend.transport.undertow.adapter.UndertowSslAdapter;
import net.magnesiumbackend.transport.undertow.handler.UndertowHttpHandler;

import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.List;

public class UndertowMagnesiumTransport implements MagnesiumTransport {
    private Undertow server;

    @Override
    public void bind(int port, MagnesiumRuntime runtime, HttpRouteRegistry routes) {
        SslConfig sslConfig = runtime.sslConfig();

        Undertow.Builder serverBuilder = Undertow.builder();
        serverBuilder.addHttpListener(port, "0.0.0.0")
            .setHandler(new UndertowHttpHandler(
                routes,
                runtime.router().globalFilters(),
                runtime.exceptionHandlerRegistry(),
                runtime.messageConverterRegistry(),
                runtime.router().webSocketRouteRegistry(),
                runtime.router().webSocketSessionManager(),
                runtime.securityHeadersFilter(),
                BackpressureExecutorResolver.resolve(runtime),
                runtime.defaultTimeout()
            ));

        if (sslConfig != null) {
            try {
                UndertowSslAdapter.applyTo(sslConfig, serverBuilder, port, "0.0.0.0");
            } catch (UnrecoverableKeyException e) {
                throw new IllegalStateException("[Magnesium] SSL keystore key is unrecoverable, wrong password?", e);
            } catch (KeyStoreException e) {
                throw new IllegalStateException("[Magnesium] SSL keystore is invalid or unsupported format.", e);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("[Magnesium] SSL algorithm unavailable on this JVM, is a security provider missing?", e);
            } catch (KeyManagementException e) {
                throw new IllegalStateException("[Magnesium] SSL context initialization failed.", e);
            }
        } else {
            serverBuilder.addHttpListener(port, "0.0.0.0");
        }

        server = serverBuilder.build();

        try {
            runtime.application().start(runtime);
        } catch (Exception e) {
            throw new IllegalStateException("Application failed during start()", e);
        }

        server.start();
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

    @Override
    public void shutdown() {
        if (server != null) {
            server.stop();
        }
    }

    @Override
    public int getPort() {
        if (server == null) {
            throw new IllegalStateException("Server not started");
        }

        List<Undertow.ListenerInfo> listenerInfo = server.getListenerInfo();
        if (!listenerInfo.isEmpty()) {
            return ((InetSocketAddress) listenerInfo.getFirst().getAddress()).getPort();
        }
        throw new IllegalStateException("No listeners registered");
    }
}