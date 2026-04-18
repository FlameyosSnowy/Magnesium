package net.magnesiumbackend.transport.tomcat;

import net.magnesiumbackend.core.MagnesiumApplication;
import net.magnesiumbackend.core.MagnesiumRuntime;
import net.magnesiumbackend.core.backpressure.BackpressureExecutorResolver;
import net.magnesiumbackend.core.http.MagnesiumTransport;
import net.magnesiumbackend.core.route.HttpRouteRegistry;
import net.magnesiumbackend.core.security.SslConfig;
import net.magnesiumbackend.transport.tomcat.adapter.TomcatSslAdapter;
import net.magnesiumbackend.transport.tomcat.websocket.TomcatWebSocketInitializer;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.websocket.server.WsSci;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import jakarta.servlet.ServletContext;
import jakarta.websocket.server.ServerContainer;

public class TomcatMagnesiumTransport implements MagnesiumTransport {
    private static final Logger LOGGER = LoggerFactory.getLogger(TomcatMagnesiumTransport.class);
    private Tomcat tomcat;

    @Override
    public void bind(int port, MagnesiumRuntime runtime, HttpRouteRegistry routes) {
        tomcat = new Tomcat();
        tomcat.setBaseDir(System.getProperty("java.io.tmpdir"));

        SslConfig sslConfig = runtime.sslConfig();
        Connector connector = new Connector();
        connector.setPort(port);

        if (sslConfig != null) {
            try {
                TomcatSslAdapter.applyTo(sslConfig, connector);
            } catch (IOException e) {
                throw new IllegalStateException("[Magnesium] Failed to write SSL keystore to temp file, check disk space and write permissions.", e);
            } catch (CertificateException e) {
                throw new IllegalStateException("[Magnesium] SSL certificate is invalid or could not be serialized.", e);
            } catch (KeyStoreException e) {
                throw new IllegalStateException("[Magnesium] SSL keystore is invalid or unsupported format.", e);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("[Magnesium] SSL algorithm unavailable on this JVM, is a security provider missing?", e);
            }
        } else {
            connector.setScheme("http");
        }

        tomcat.getService().addConnector(connector);

        Executor executor = BackpressureExecutorResolver.resolve(runtime);
        tomcat.getConnector().getProtocolHandler().setExecutor(executor);

        Context context = tomcat.addContext("", new File(".").getAbsolutePath());

        context.addServletContainerInitializer(new WsSci(), null);

        MagnesiumTomcatServlet servlet = new MagnesiumTomcatServlet(
            routes,
            runtime.router().globalFilters(),
            runtime.exceptionHandlerRegistry(),
            runtime.messageConverterRegistry(),
            runtime.securityHeadersFilter(),
            executor,
            runtime.defaultTimeout()
        );
        tomcat.addServlet("", "magnesiumServlet", servlet);
        context.addServletMappingDecoded("/*", "magnesiumServlet");

        try {
            initializeWebSockets(context, runtime);

            try {
                runtime.application().start(runtime);
            } catch (Exception e) {
                throw new IllegalStateException("Application failed during start()", e);
            }

            tomcat.start();
            try {
                runtime.application().ready(runtime, getPort());
            } catch (Exception e) {
                throw new RuntimeException("[Magnesium] Tomcat transport interrupted by an error from Application#ready.", e);
            }

            runtime.shutdownLatch().await();
        } catch (LifecycleException e) {
            throw new IllegalStateException("[Magnesium] Tomcat failed to start.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private void initializeWebSockets(Context context, MagnesiumRuntime application) {
        ServletContext servletContext = context.getServletContext();
        ServerContainer serverContainer = (ServerContainer) servletContext
            .getAttribute("jakarta.websocket.server.ServerContainer");

        if (serverContainer == null) {
            LOGGER.warn("[Magnesium] WebSocket ServerContainer not available, WebSocket routes will not be registered.");
            return;
        }

        try {
            new TomcatWebSocketInitializer(
                application.router().webSocketRouteRegistry(),
                application.router().webSocketSessionManager()
            ).initialize(serverContainer);
        } catch (Exception e) {
            throw new IllegalStateException("[Magnesium] Failed to register WebSocket routes.", e);
        }
    }

    @Override
    public void shutdown() {
        if (tomcat != null) {
            try {
                tomcat.stop();
                tomcat.destroy();
            } catch (LifecycleException e) {
                LOGGER.error("Error shutting down Tomcat", e);
            }
        }
    }

    @Override
    public int getPort() {
        return tomcat.getConnector().getPort();
    }
}