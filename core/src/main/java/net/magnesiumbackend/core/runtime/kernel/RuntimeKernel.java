package net.magnesiumbackend.core.runtime.kernel;

import net.magnesiumbackend.core.runtime.config.RuntimeConfig;
import net.magnesiumbackend.core.runtime.engine.CommandExecutor;
import net.magnesiumbackend.core.runtime.input.InputSource;
import net.magnesiumbackend.core.runtime.lifecycle.LifecyclePolicy;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runtime kernel for the Magnesium framework.
 *
 * <p>Orchestrates input sources, command execution, and lifecycle management.</p>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * RuntimeConfig config = RuntimeConfig.builder()
 *     .inputSource(new ShellInputSource())
 *     .inputSource(new HttpInputSource(8080))
 *     .lifecyclePolicy(new LatchLifecyclePolicy())
 *     .build();
 *
 * RuntimeKernel kernel = new RuntimeKernel(config, commandExecutor);
 * kernel.start(); // Blocks until shutdown
 * }</pre>
 */
public final class RuntimeKernel {

    private static final Logger logger = LoggerFactory.getLogger(RuntimeKernel.class);

    private final RuntimeConfig config;
    private final CommandExecutor executor;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean shutdownRequested = new AtomicBoolean(false);

    private final LifecyclePolicy.ShutdownContext shutdownContext =
        new LifecyclePolicy.ShutdownContext() {
            @Override
            public void requestShutdown() {
                shutdownRequested.set(true);
                RuntimeKernel.this.shutdown();
            }

            @Override
            public boolean isShutdownRequested() {
                return shutdownRequested.get();
            }
        };

    /**
     * Creates a runtime kernel with the given configuration.
     *
     * @param config the runtime configuration
     * @param executor the command executor
     */
    public RuntimeKernel(@NotNull RuntimeConfig config, @NotNull CommandExecutor executor) {
        this.config = config;
        this.executor = executor;
    }

    /**
     * Starts the runtime kernel.
     *
     * <p>1. Starts all input sources</p>
     * <p>2. Blocks according to lifecycle policy</p>
     * <p>3. Stops all input sources on shutdown</p>
     *
     * <p>This method blocks until shutdown is requested.</p>
     */
    public void start() {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Runtime kernel is already running");
        }

        logger.info("Starting RuntimeKernel with {} input sources", config.inputSources().size());

        // Start all input sources
        for (InputSource source : config.inputSources()) {
            try {
                logger.debug("Starting input source: {}", source.name());
                source.start(executor);
            } catch (Exception e) {
                logger.error("Failed to start input source: {}"
, e);
                throw new RuntimeException("Failed to start input source: " + source.name(), e);
            }
        }

        logger.info("RuntimeKernel started, entering main loop");

        // Block according to lifecycle policy
        config.lifecyclePolicy().blockUntilShutdown(shutdownContext);

        // Shutdown sequence
        shutdown();
    }

    /**
     * Requests graceful shutdown.
     */
    public void shutdown() {
        if (!running.get()) {
            return;
        }

        logger.info("Shutting down RuntimeKernel...");
        shutdownRequested.set(true);

        // Stop all input sources
        for (InputSource source : config.inputSources()) {
            try {
                logger.debug("Stopping input source: {}", source.name());
                source.stop();
            } catch (Exception e) {
                logger.warn("Error stopping input source: {}", source.name(), e);
            }
        }

        running.set(false);
        logger.info("RuntimeKernel shutdown complete");
    }

    /**
     * Returns true if the kernel is running.
     *
     * @return running status
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Returns the runtime configuration.
     *
     * @return the configuration
     */
    public @NotNull RuntimeConfig config() {
        return config;
    }
}
