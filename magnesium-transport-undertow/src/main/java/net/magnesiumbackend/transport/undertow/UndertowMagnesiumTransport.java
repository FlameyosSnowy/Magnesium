package net.magnesiumbackend.transport.undertow;

import io.undertow.Undertow;
import net.magnesiumbackend.core.MagnesiumApplication;
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
    public void bind(int port, MagnesiumApplication application, HttpRouteRegistry routes) {
        SslConfig sslConfig = application.sslConfig();

        Undertow.Builder serverBuilder = Undertow.builder();
        serverBuilder.addHttpListener(port, "0.0.0.0")
            .setHandler(new UndertowHttpHandler(
                routes,
                application.httpServer().globalFilters(),
                application.exceptionHandlerRegistry(),
                application.messageConverterRegistry(),
                application.httpServer().webSocketRouteRegistry(),
                application.httpServer().webSocketSessionManager(),
                application.securityHeadersFilter(),
                application.executor()
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

        server.start();
        application.onStart().accept(application.serviceRegistry());

        try {
            application.shutdownLatch().await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
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