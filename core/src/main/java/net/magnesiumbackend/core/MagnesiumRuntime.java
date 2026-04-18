package net.magnesiumbackend.core;

import net.magnesiumbackend.core.backpressure.BackpressureConfig;
import net.magnesiumbackend.core.config.MagnesiumConfigurationManager;
import net.magnesiumbackend.core.event.EventBus;
import net.magnesiumbackend.core.exceptions.ExceptionHandlerRegistrar;
import net.magnesiumbackend.core.exceptions.ExceptionHandlerRegistry;
import net.magnesiumbackend.core.http.MagnesiumTransport;
import net.magnesiumbackend.core.http.RequestSecurityRegistry;
import net.magnesiumbackend.core.http.RequestSecurityRegistryBuilder;
import net.magnesiumbackend.core.http.messages.MessageConverterRegistry;
import net.magnesiumbackend.core.json.JsonProvider;
import net.magnesiumbackend.core.security.SecurityHeadersFilter;
import net.magnesiumbackend.core.security.SslConfig;
import net.magnesiumbackend.core.services.ServiceContext;
import net.magnesiumbackend.core.services.ServiceRegistrar;
import net.magnesiumbackend.core.services.ServiceRegistry;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Mutable configuration surface for a Magnesium application.
 *
 * <p>An instance is created by the framework and passed to
 * {@link Application#configure(MagnesiumRuntime)} exactly once, before any
 * server threads start. After {@code configure} returns the runtime is
 * <em>frozen</em>: all mutating methods throw {@link IllegalStateException}.
 *
 * <p>Example usage inside {@code Application.configure}:
 * <pre>{@code
 * @Override
 * protected void configure(MagnesiumRuntime runtime) {
 *     runtime.executor(Executors.newVirtualThreadPerTaskExecutor());
 *     runtime.json(new JacksonJsonProvider());
 *     runtime.services(s -> s.register(OrderService.class, ctx -> new OrderService()));
 *     runtime.http(http -> http.route(GET, "/health", req -> Response.ok("up")));
 * }
 * }</pre>
 */
public class MagnesiumRuntime {
    final Application application;
    final EventBus eventBus = new EventBus();
    final ExceptionHandlerRegistry exceptionHandlerRegistry = new ExceptionHandlerRegistry();

    Executor executor;
    JsonProvider jsonProvider;
    MagnesiumTransport transport;
    Router router;
    MessageConverterRegistry messageConverterRegistry;
    ServiceRegistry serviceRegistry;
    MagnesiumConfigurationManager configurationManager =
        MagnesiumConfigurationManager.builder().build();

    @Nullable RequestSecurityRegistry requestSecurityRegistry;
    @Nullable BackpressureConfig backpressureConfig;
    SslConfig sslConfig;
    Duration defaultTimeout = Duration.ofSeconds(30);

    private final Map<Class<?>, Function<ServiceContext, ?>> serviceFactories = new HashMap<>(16);
    private boolean frozen = false;
    private @NotNull SecurityHeadersFilter securityHeaderFilter;
    final CountDownLatch shutdownLatch = new CountDownLatch(1);

    public MagnesiumRuntime(Application application) {
        this.application = application;
    }

    public Application application() {
        return application;
    }

    /** Sets the executor used for route handling and event listeners. */
    public MagnesiumRuntime executor(@NotNull Executor executor) {
        ensureNotFrozen();
        this.executor = executor;
        return this;
    }

    /** Overrides the JSON provider (also available as {@link #jsonProvider(JsonProvider)}). */
    public MagnesiumRuntime json(@NotNull JsonProvider provider) {
        ensureNotFrozen();
        this.jsonProvider = provider;
        return this;
    }

    /** Alias for {@link #json(JsonProvider)}. */
    public MagnesiumRuntime jsonProvider(@NotNull JsonProvider provider) {
        return json(provider);
    }

    /** Overrides the network transport (defaults to whatever is on the classpath). */
    public MagnesiumRuntime transport(@NotNull MagnesiumTransport transport) {
        ensureNotFrozen();
        this.transport = transport;
        return this;
    }

    /**
     * The router for configuring HTTP server's routes, filters, etc.
     *
     * <pre>{@code
     * runtime.router()
     *     .get("/ping", req -> Response.ok("pong"))
     *          .filter(...)
     *          .commit()
     *     .post(...)
     *          .commit()
     * }</pre>
     */
    public Router router() {
        return router;
    }

    /**
     * Registers application services.
     *
     * <pre>{@code
     * runtime.services(s -> s
     *     .register(OrderService.class, ctx -> new OrderService(db)));
     * }</pre>
     */
    @Contract("_ -> this")
    public MagnesiumRuntime services(@NotNull Consumer<ServiceRegistrar> configure) {
        ensureNotFrozen();
        ServiceRegistrar registrar = new ServiceRegistrar(serviceFactories);
        configure.accept(registrar);
        this.serviceRegistry = ServiceRegistry.from(registrar, eventBus);
        return this;
    }

    /**
     * Registers exception handlers, both global and per-controller.
     *
     * <pre>{@code
     * runtime.exceptions(ex -> ex
     *     .global(ValidationException.class, (e, req) ->
     *         Response.status(400).body(e.getMessage()).build())
     *     .fallback((e, req) ->
     *         Response.status(500).body("Unhandled error").build()));
     * }</pre>
     */
    @Contract("_ -> this")
    public MagnesiumRuntime exceptions(@NotNull Consumer<ExceptionHandlerRegistrar> configure) {
        ensureNotFrozen();
        configure.accept(new ExceptionHandlerRegistrar(exceptionHandlerRegistry));
        return this;
    }

    /** Configures message body converters. */
    @Contract("_ -> this")
    public MagnesiumRuntime messageConversion(@NotNull Consumer<MessageConverterRegistry> configure) {
        ensureNotFrozen();
        if (this.messageConverterRegistry == null) {
            this.messageConverterRegistry = MessageConverterRegistry.withDefaults(jsonProvider);
        }
        configure.accept(messageConverterRegistry);
        return this;
    }

    /** Overrides the application configuration manager. */
    @Contract("_ -> this")
    public MagnesiumRuntime configuration(
        @NotNull Consumer<MagnesiumConfigurationManager.Builder> configure) {
        ensureNotFrozen();
        MagnesiumConfigurationManager.Builder b = MagnesiumConfigurationManager.builder();
        configure.accept(b);
        this.configurationManager = b.build();
        return this;
    }

    /** Configures TLS. */
    public MagnesiumRuntime ssl(@NotNull SslConfig sslConfig) {
        ensureNotFrozen();
        this.sslConfig = sslConfig;
        return this;
    }

    /** Overrides the default 30-second request timeout. */
    public MagnesiumRuntime requestTimeout(@NotNull Duration timeout) {
        ensureNotFrozen();
        this.defaultTimeout = timeout;
        return this;
    }

    /**
     * Enables bounded-queue backpressure on the request executor.
     *
     * <pre>{@code
     * runtime.backpressure(bp -> bp
     *     .queueCapacity(512)
     *     .onReject(RejectionResponse.of(503)
     *         .withBody("Server busy, please retry")
     *         .withRetryAfter(Duration.ofSeconds(5))));
     * }</pre>
     */
    @Contract("_ -> this")
    public MagnesiumRuntime backpressure(@NotNull Consumer<BackpressureConfig.Builder> configure) {
        ensureNotFrozen();
        BackpressureConfig.Builder b = BackpressureConfig.builder();
        configure.accept(b);
        this.backpressureConfig = b.build();
        return this;
    }

    /** Configures request signing and security headers. */
    @Contract("_ -> this")
    public MagnesiumRuntime requestSecurity(
        @NotNull Consumer<RequestSecurityRegistryBuilder> configure) {
        ensureNotFrozen();
        RequestSecurityRegistryBuilder b = new RequestSecurityRegistryBuilder();
        configure.accept(b);
        this.requestSecurityRegistry = b.build();
        return this;
    }

    /** Configures request signing and security headers. */
    @Contract("_ -> this")
    public MagnesiumRuntime securityHeadersFilter(
        @NotNull SecurityHeadersFilter securityHeadersFilter) {
        ensureNotFrozen();
        this.securityHeaderFilter = securityHeadersFilter;
        return this;
    }

    public @NotNull SecurityHeadersFilter securityHeadersFilter() {
        return securityHeaderFilter;
    }

    public ServiceRegistry serviceRegistry() {
        return serviceRegistry;
    }

    public EventBus eventBus() {
        return eventBus;
    }

    public ExceptionHandlerRegistry exceptionHandlerRegistry() {
        return exceptionHandlerRegistry;
    }

    public MagnesiumConfigurationManager configurationManager() {
        return configurationManager;
    }

    public boolean isFrozen() {
        return frozen;
    }

    public Executor executor() {
        return executor;
    }

    public JsonProvider jsonProvider() {
        return jsonProvider;
    }

    public MagnesiumTransport transport() {
        return transport;
    }

    public MessageConverterRegistry messageConverterRegistry() {
        return messageConverterRegistry;
    }

    public @Nullable RequestSecurityRegistry requestSecurityRegistry() {
        return requestSecurityRegistry;
    }

    public @Nullable BackpressureConfig backpressureConfig() {
        return backpressureConfig;
    }

    public SslConfig sslConfig() {
        return sslConfig;
    }

    public Duration defaultTimeout() {
        return defaultTimeout;
    }

    public Map<Class<?>, Function<ServiceContext, ?>> serviceFactories() {
        return serviceFactories;
    }

    // ── lifecycle (package-private) ───────────────────────────────────────────

    /**
     * Called by {@link MagnesiumApplication} after {@link Application#configure} returns.
     * Seals the runtime so no further mutation is possible, and materialises any
     * defaults that depend on other settings (e.g. message converters need the JSON provider).
     */
    public void freeze() {
        if (frozen) return;
        // Materialise defaults that couldn't be resolved earlier
        if (this.messageConverterRegistry == null) {
            this.messageConverterRegistry = MessageConverterRegistry.withDefaults(jsonProvider);
        }
        this.frozen = true;
    }

    private void ensureNotFrozen() {
        if (frozen) {
            throw new IllegalStateException(
                "MagnesiumRuntime is already frozen — configure() has returned. " +
                    "All configuration must happen inside Application#configure(MagnesiumRuntime).");
        }
    }

    public CountDownLatch shutdownLatch() {
        return shutdownLatch;
    }
}