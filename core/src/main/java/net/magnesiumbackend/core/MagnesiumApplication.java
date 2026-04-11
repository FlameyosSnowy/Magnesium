package net.magnesiumbackend.core;

import net.magnesiumbackend.core.config.MagnesiumConfigurationManager;
import net.magnesiumbackend.core.event.EventBus;
import net.magnesiumbackend.core.http.MagnesiumHttpServer;
import net.magnesiumbackend.core.http.MagnesiumTransport;
import net.magnesiumbackend.core.http.RequestSecurityRegistry;
import net.magnesiumbackend.core.http.RequestSecurityRegistryBuilder;
import net.magnesiumbackend.core.http.messages.MessageConverterRegistry;
import net.magnesiumbackend.core.json.JsonProvider;
import net.magnesiumbackend.core.meta.GeneratedExceptionHandlers;
import net.magnesiumbackend.core.exceptions.ExceptionHandlerRegistrar;
import net.magnesiumbackend.core.exceptions.ExceptionHandlerRegistry;
import net.magnesiumbackend.core.services.ServiceContext;
import net.magnesiumbackend.core.services.ServiceRegistrar;
import net.magnesiumbackend.core.services.ServiceRegistry;
import net.magnesiumbackend.core.security.RequestSigningFilter;
import net.magnesiumbackend.core.security.SecurityHeadersFilter;
import net.magnesiumbackend.core.security.SslConfig;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Entry point for a Magnesium application.
 *
 * <p>All configuration is done through {@link Builder}. Calling {@link Builder#build()}
 * seals the application, no further mutation is possible. The only post-build
 * operation is {@link #run(int)}, which starts the HTTP server and blocks until shutdown.
 *
 * <pre>{@code
 * MagnesiumApplication.builder()
 *     .execution(ExecutionStrategy.ofExecutor(...))
 *     .json(new JacksonJsonProvider())
 *     .services(s -> s
 *         .register(OrderService.class, ctx -> new OrderService(db)))
 *     .controllers(c -> c
 *         .mount(ctx -> new OrderController(ctx.get(OrderService.class))))
 *     .http(http -> http
 *         .route(GET, "/health", req -> Response.ok("up")))
 *     .onStart(ctx -> { ... })
 *     .onExit(ctx  -> { ... })
 *     .build()
 *     .run(8080);
 * }</pre>
 */
@SuppressWarnings("unused")
public final class MagnesiumApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(MagnesiumApplication.class);

    private final Executor executor;
    private final JsonProvider jsonProvider;
    private final MagnesiumHttpServer httpServer;
    private final ServiceRegistry serviceRegistry;
    private final EventBus eventBus;
    private final MagnesiumConfigurationManager configurationManager;
    private final Consumer<ServiceContext> onStart;
    private final Consumer<ServiceContext> onExit;
    private final MagnesiumTransport transport;
    private final ExceptionHandlerRegistry exceptionHandlerRegistry;
    private final MessageConverterRegistry messageConverterRegistry;
    private final SslConfig sslConfig;

    @Nullable
    private final RequestSecurityRegistry requestSecurityRegistry;

    private MagnesiumApplication(Builder builder) {
        this.eventBus = builder.eventBus;
        this.serviceRegistry = builder.serviceRegistry;
        this.configurationManager = builder.configurationManager;

        this.executor = builder.executor;
        this.jsonProvider = builder.jsonProvider;
        this.httpServer = builder.httpServer;

        this.exceptionHandlerRegistry = builder.exceptionHandlerRegistry;
        this.messageConverterRegistry = builder.messageConverterRegistry;

        this.requestSecurityRegistry = builder.requestSecurityRegistry;
        this.sslConfig = builder.sslConfig;

        this.onStart = builder.onStart != null ? builder.onStart : _ -> {};
        this.onExit  = builder.onExit  != null ? builder.onExit  : _ -> {};

        this.transport = TransportLoader.load().orElseGet(() -> {
            if (httpServer.isConfigured()) {
                throw new IllegalStateException("No TransportProvider found on classpath but HttpServer is configured. Add a dependency like magnesium-transport-netty.");
            }
            return null;
        });

        MagnesiumBootstrap.load().apply(this);

        GeneratedExceptionHandlers.GLOBAL.forEach(exceptionHandlerRegistry::registerGlobal);
        GeneratedExceptionHandlers.LOCAL.forEach(exceptionHandlerRegistry::registerRoute);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Executor executor() {
        return executor;
    }

    public JsonProvider jsonProvider() {
        return jsonProvider;
    }

    public MagnesiumHttpServer httpServer() {
        return httpServer;
    }

    public ServiceRegistry serviceRegistry() {
        return serviceRegistry;
    }

    public EventBus eventBus() {
        return eventBus;
    }

    public SslConfig sslConfig() {
        return sslConfig;
    }

    public MagnesiumConfigurationManager configurationManager() {
        return configurationManager;
    }

    public Consumer<ServiceContext> onStart() {
        return onStart;
    }

    public Consumer<ServiceContext> onExit() {
        return onExit;
    }

    public MagnesiumTransport transport() {
        return transport;
    }

    public CountDownLatch shutdownLatch() {
        return shutdownLatch;
    }

    public MessageConverterRegistry messageConverterRegistry() {
        return messageConverterRegistry;
    }

    public RequestSigningFilter requestSigningFilter() {
        if (requestSecurityRegistry != null) {
            return requestSecurityRegistry.signingFilter();
        }

        throw new IllegalStateException("Request signing has been requested but requestSecurityRegistry is null. Did you forget to configure MagnesiumApplication.Builder#requestSecurity?");
    }

    public SecurityHeadersFilter securityHeadersFilter() {
        return requestSecurityRegistry != null ? requestSecurityRegistry.securityHeadersFilter() : null;
    }

    public static final class Builder {
        private SslConfig sslConfig;
        private MessageConverterRegistry messageConverterRegistry;
        private Executor executor;
        private JsonProvider jsonProvider;
        private MagnesiumHttpServer httpServer;
        private final Map<Class<?>, Function<ServiceContext, ?>> serviceFactories = new HashMap<>(16);
        private final ExceptionHandlerRegistry exceptionHandlerRegistry           = new ExceptionHandlerRegistry();
        private Consumer<ServiceContext> onStart;
        private Consumer<ServiceContext> onExit;
        private final EventBus eventBus = new EventBus();
        private ServiceRegistry serviceRegistry;
        private MagnesiumConfigurationManager configurationManager = MagnesiumConfigurationManager.builder().build();
        private RequestSecurityRegistry requestSecurityRegistry;

        private Builder() {}

        /** Sets the executor used for route handling and event listeners. */
        public Builder execution(Executor executor) {
            this.executor = executor;
            return this;
        }

        /** Overrides the default JSON provider. */
        public Builder json(JsonProvider provider) {
            this.jsonProvider = provider;
            return this;
        }

        /** Registers application services with the service services. */
        @Contract("_ -> this")
        public Builder services(@NotNull Consumer<ServiceRegistrar> configure) {
            ServiceRegistrar serviceRegistrar = new ServiceRegistrar(serviceFactories);
            configure.accept(serviceRegistrar);
            this.serviceRegistry = ServiceRegistry.from(serviceRegistrar, eventBus);
            return this;
        }

        @Contract("_ -> this")
        public Builder configuration(@NotNull Consumer<MagnesiumConfigurationManager.Builder> configure) {
            MagnesiumConfigurationManager.Builder builder = MagnesiumConfigurationManager.builder();
            configure.accept(builder);
            this.configurationManager = builder.build();
            return this;
        }

        public Builder ssl(SslConfig sslConfig) {
            this.sslConfig = sslConfig;
            return this;
        }

        /**
         * Registers exception handlers, both global and per-controller.
         *
         * <p>Handlers registered here are merged with (and take precedence over)
         * any handlers discovered by the annotation processor at compile time.
         * Most-specific type wins when multiple handlers could match.
         *
         * <pre>{@code
         * .exceptions(ex -> ex
         *     .global(ValidationException.class, (e, req) ->
         *         Response.status(400).body(e.getMessage()).build())
         *     .local(OrderController.class, NotFoundException.class, (e, req) ->
         *         Response.status(404).body("Order not found").build())
         *     .fallback((e, req) ->
         *         Response.status(500).body("Unhandled error").build())
         * )
         * }</pre>
         */
        @Contract("_ -> this")
        public Builder exceptions(@NotNull Consumer<ExceptionHandlerRegistrar> configure) {
            configure.accept(new ExceptionHandlerRegistrar(exceptionHandlerRegistry));
            return this;
        }

        /** Configures the embedded HTTP server. */
        @Contract("_ -> this")
        public Builder http(@NotNull Consumer<MagnesiumHttpServer.Builder> configure) {
            MagnesiumHttpServer.Builder b = MagnesiumHttpServer.builder();
            configure.accept(b);
            this.httpServer = b.build();
            return this;
        }

        /** Configures the embedded HTTP server. */
        @Contract("_ -> this")
        public Builder messageConversion(@NotNull Consumer<MessageConverterRegistry> configure) {
            configure.accept(messageConverterRegistry);
            return this;
        }

        /** Callback invoked once the server is bound and ready to accept requests. */
        public Builder onStart(Consumer<ServiceContext> onStart) {
            this.onStart = onStart;
            return this;
        }

        /** Callback invoked during graceful shutdown (not on hard kill). */
        public Builder onExit(Consumer<ServiceContext> onExit) {
            this.onExit = onExit;
            return this;
        }

        public Builder requestSecurity(Consumer<RequestSecurityRegistryBuilder> consumer) {
            RequestSecurityRegistryBuilder builder = new RequestSecurityRegistryBuilder();
            consumer.accept(builder);
            this.requestSecurityRegistry = builder.build();
            return this;
        }

        public ServiceRegistry serviceRegistry() {
            return serviceRegistry;
        }

        /**
         * Validates configuration and builds the sealed {@link MagnesiumApplication}.
         * The builder must not be used after this call.
         */
        @NotNull
        @Contract(" -> new")
        public MagnesiumApplication build() {
            if (this.messageConverterRegistry == null) {
                this.messageConverterRegistry = MessageConverterRegistry.withDefaults(jsonProvider);
            }
            return new MagnesiumApplication(this);
        }
    }

    private final CountDownLatch shutdownLatch = new CountDownLatch(1);

    /**
     * Starts the HTTP server on {@code port}, runs {@code onStart}, then <strong>blocks</strong>
     * until the process receives a shutdown signal, at which point {@code onExit} runs.
     *
     * @param port the TCP port to listen on
     */
    public void run(int port) {
        LOGGER.info("Starting the server on port: {}", port);

        eventBus.start();

        MagnesiumStartupLogger.logStartup(this);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            onExit.accept(serviceRegistry);
            shutdownLatch.countDown();
        }));

        bindServer(port, shutdownLatch);
    }

    private void bindServer(int port, CountDownLatch shutdownLatch) {
        // If the transport is null AND the http server is NOT null then we'll throw
        if (transport == null && httpServer != null) {
            throw new IllegalStateException("Cannot have a null MagnesiumTransport while having a configured HTTP server.");
        }

        // If the transport is NOT null AND the http server is null then we'll throw
        if (transport != null && httpServer == null) {
            throw new IllegalStateException("Cannot have a null HTTP server while having a configured MagnesiumTransport.");
        }

        // If we're here then it means that the transport can either be not-null and http server is not-null as well, or the opposite.
        // If the transport is null then the HTTP server is null, then we have no server to bind a port to.

        //noinspection ConstantValue
        if (this.transport == null && this.httpServer == null) {
            // By contract, we should still block.
            try {
                this.shutdownLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return;
        }

        transport.bind(port, this, this.httpServer.routes());
    }

    public ExceptionHandlerRegistry exceptionHandlerRegistry() {
        return exceptionHandlerRegistry;
    }
}