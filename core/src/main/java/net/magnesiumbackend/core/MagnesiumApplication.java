package net.magnesiumbackend.core;

import net.magnesiumbackend.core.extensions.MagnesiumExtension;
import net.magnesiumbackend.core.json.JsonProviderLoader;
import net.magnesiumbackend.core.meta.GeneratedExceptionHandlers;
import net.magnesiumbackend.core.security.RequestSigningFilter;
import net.magnesiumbackend.core.security.SecurityHeadersFilter;
import net.magnesiumbackend.core.services.ServiceBootstrap;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CountDownLatch;

/**
 * Framework entry point.
 *
 * <p>Create a subclass of {@link Application}, implement {@link Application#configure},
 * and hand it to {@link #run(Application, int)}:
 *
 * <pre>{@code
 * public static void main(String[] args) {
 *     MagnesiumApplication.run(new MainApplication(), 8080);
 * }
 * }</pre>
 *
 * <p>This class is intentionally not instantiable by user code — it exists only as
 * a namespace for the static {@code run} method and as the internal orchestrator
 * that wires {@link MagnesiumRuntime} to the transport layer.
 */
@SuppressWarnings("unused")
public final class MagnesiumApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(MagnesiumApplication.class);

    // Not instantiable
    public MagnesiumApplication() {

    }

    /**
     * Configures, bootstraps, and starts the application, then blocks until shutdown.
     *
     * <p>Lifecycle:
     * <ol>
     *   <li>A fresh {@link MagnesiumRuntime} is created and passed to
     *       {@link Application#configure(MagnesiumRuntime)}.</li>
     *   <li>The runtime is frozen; no further mutation is allowed.</li>
     *   <li>Classpath bootstrap ({@link MagnesiumBootstrap}) and generated exception
     *       handlers are applied.</li>
     *   <li>The transport binds {@code port}.</li>
     *   <li>{@link Application#start(MagnesiumRuntime)} is invoked.</li>
     *   <li>This method blocks until a shutdown signal is received.</li>
     *   <li>{@link Application#stop(MagnesiumRuntime)} is invoked.</li>
     * </ol>
     *
     * @param application the user application
     * @param port        the TCP port to listen on
     */
    public static void run(@NotNull Application application, int port) {
        MagnesiumRuntime runtime = new MagnesiumRuntime(application);

        // Auto-load extensions via ServiceLoader before application.configure()
        LOGGER.info("Loading Magnesium extensions via ServiceLoader.");
        ServiceLoader<MagnesiumExtension> extensionLoader = ServiceLoader.load(MagnesiumExtension.class);
        List<MagnesiumExtension> extensions = new ArrayList<>();
        for (MagnesiumExtension extension : extensionLoader) {
            extensions.add(extension);
            LOGGER.info("Discovered extension: {} ({})", extension.name(), extension.getClass().getName());
        }

        // Sort extensions by name for deterministic ordering
        extensions.sort(Comparator.comparing(MagnesiumExtension::name));

        // Configure all discovered extensions
        for (MagnesiumExtension extension : extensions) {
            LOGGER.info("Configuring extension: {}", extension.name());
            extension.configure(runtime);
        }

        application.configure(runtime);

        runtime.freeze();

        LOGGER.info("Loading JSON Provider.");
        if (runtime.jsonProvider == null) {
            JsonProviderLoader.load().ifPresentOrElse(
                (jsonProvider) -> {
                    runtime.jsonProvider = jsonProvider;
                    LOGGER.info("A JsonProvider of type: {} has been provided via dependency graph.", jsonProvider.getClass());
                },
                () -> LOGGER.info("No JsonProvider has been found, a JsonProvider is required for conversion of objects to user DTOs. " +
                    "Please add a JsonProvider dependency if you need this feature, i.e. magnesium-dsljson-json-provider."));

        } else {
            LOGGER.info("JSON Provider of type: {} has been provided manually.", runtime.jsonProvider.getClass());
        }

        // Resolve transport from classpath if the user didn't set one
        if (runtime.transport == null) {
            TransportLoader.load().ifPresentOrElse(
                (transport) -> {
                    runtime.transport = transport;
                    LOGGER.info("A MagnesiumTransport of type {} has been provided via dependency graph.", transport.getClass());
                },
                () -> {
                    if (runtime.router.isConfigured()) {
                        throw new IllegalStateException(
                            "No TransportProvider found on classpath but an HttpServer is " +
                                "configured. Add a dependency such as magnesium-transport-netty.");
                    }
                });
        } else {
            LOGGER.info("A MagnesiumTransport of type {} has been provided manually.", runtime.transport.getClass());
        }

        LOGGER.info("Auto-wiring magnesium services annotated with RestService.");
        runtime.services(services -> ServiceBootstrap.wireAll(runtime, services));
        
        MagnesiumBootstrap.load().apply(runtime);

        GeneratedExceptionHandlers.GLOBAL
            .forEach(runtime.exceptionHandlerRegistry::registerGlobal);
        GeneratedExceptionHandlers.LOCAL
            .forEach(runtime.exceptionHandlerRegistry::registerRoute);

        MagnesiumStartupLogger.logStartup(runtime);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                application.stop(runtime);
            } catch (Exception e) {
                LOGGER.error("Exception during application stop", e);
            } finally {
                runtime.shutdownLatch.countDown();
            }
        }));

        bindTransport(runtime, port, runtime.shutdownLatch);
    }

    static RequestSigningFilter requestSigningFilter(@NotNull MagnesiumRuntime runtime) {
        if (runtime.requestSecurityRegistry != null) {
            return runtime.requestSecurityRegistry.signingFilter();
        }
        throw new IllegalStateException(
            "Request signing has been requested but requestSecurityRegistry is null. " +
                "Did you forget to call MagnesiumRuntime#requestSecurity(...)?");
    }

    static SecurityHeadersFilter securityHeadersFilter(@NotNull MagnesiumRuntime runtime) {
        return runtime.requestSecurityRegistry != null
            ? runtime.requestSecurityRegistry.securityHeadersFilter()
            : null;
    }

    private static void bindTransport(
        MagnesiumRuntime runtime, int port, CountDownLatch shutdownLatch) {

        boolean hasTransport  = runtime.transport  != null;

        if (!hasTransport) {
            // No HTTP at all, still honor the blocking contract
            LOGGER.warn("No transport or HTTP server configured; " +
                "server will block without accepting connections.");
            try {
                shutdownLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return;
        }

        runtime.transport.bind(port, runtime, runtime.router.routes());
    }
}