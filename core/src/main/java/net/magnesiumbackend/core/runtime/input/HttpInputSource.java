package net.magnesiumbackend.core.runtime.input;

import net.magnesiumbackend.core.MagnesiumRuntime;
import net.magnesiumbackend.core.http.MagnesiumTransport;
import net.magnesiumbackend.core.runtime.engine.CommandExecutor;
import net.magnesiumbackend.core.runtime.lifecycle.LifecyclePolicy;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Input source for HTTP transport.
 *
 * <p>Wraps an HTTP server (Tomcat, Undertow, Netty, etc.) as an input source.
 * The HTTP server runs in its own virtual thread and processes incoming
 * requests as commands.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * // In application configuration:
 * RuntimeConfig config = RuntimeConfig.builder()
 *     .inputSource(new HttpInputSource(8080, transport, runtime, routes))
 *     .lifecyclePolicy(new LatchLifecyclePolicy())
 *     .build();
 *
 * RuntimeKernel kernel = new RuntimeKernel(config, executor);
 * kernel.start();
 * }</pre>
 *
 * <p>This replaces the hardcoded shutdown latch pattern in HTTP transports.</p>
 */
public final class HttpInputSource implements InputSource, LifecyclePolicy.ShutdownContext {

    private static final Logger logger = Logger.getLogger(HttpInputSource.class.getName());

    private final int port;
    private final MagnesiumTransport transport;
    private final MagnesiumRuntime runtime;
    private final net.magnesiumbackend.core.route.HttpRouteRegistry routes;

    private volatile boolean running = false;
    private volatile CommandExecutor executor;
    private ExecutorService serverExecutor;
    private CountDownLatch shutdownLatch;

    /**
     * Creates an HTTP input source.
     *
     * @param port the port to listen on
     * @param transport the HTTP transport implementation
     * @param runtime the Magnesium runtime
     * @param routes the HTTP route registry
     */
    public HttpInputSource(
        int port,
        @NotNull MagnesiumTransport transport,
        @NotNull MagnesiumRuntime runtime,
        @NotNull net.magnesiumbackend.core.route.HttpRouteRegistry routes
    ) {
        this.port = port;
        this.transport = transport;
        this.runtime = runtime;
        this.routes = routes;
    }

    @Override
    public void start(@NotNull CommandExecutor executor) {
        this.executor = executor;
        this.running = true;
        this.shutdownLatch = new CountDownLatch(1);

        // Start HTTP server in a virtual thread
        this.serverExecutor = Executors.newVirtualThreadPerTaskExecutor();
        serverExecutor.submit(this::runServer);

        logger.info("HttpInputSource started on port " + port);
    }

    @Override
    public void stop() {
        running = false;
        requestShutdown();

        if (serverExecutor != null) {
            serverExecutor.shutdownNow();
        }

        // Shutdown the underlying transport
        if (transport instanceof net.magnesiumbackend.core.lifecycle.Stoppable stoppable) {
            try {
                stoppable.stop();
            } catch (Exception e) {
                logger.warning("Error stopping transport: " + e.getMessage());
            }
        }

        logger.info("HttpInputSource stopped");
    }

    private void runServer() {
        try {
            logger.info("Starting HTTP server on port " + port);
            transport.bind(port, runtime, routes);
        } catch (Exception e) {
            logger.severe("HTTP server failed: " + e.getMessage());
            throw new RuntimeException("HTTP server failed", e);
        } finally {
            shutdownLatch.countDown();
        }
    }

    /**
     * Blocks until the HTTP server shuts down.
     *
     * <p>This can be used with {@link net.magnesiumbackend.core.runtime.lifecycle.LifecyclePolicy}</p>
     */
    public void awaitShutdown() throws InterruptedException {
        shutdownLatch.await();
    }

    @Override
    public void requestShutdown() {
        shutdownLatch.countDown();
    }

    @Override
    public boolean isShutdownRequested() {
        return shutdownLatch.getCount() == 0;
    }

    @Override
    public @NotNull String name() {
        return "http-" + port;
    }

    public int port() {
        return port;
    }
}
