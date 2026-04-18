package net.magnesiumbackend.core;

/**
 * Base class for a Magnesium application.
 *
 * <p>Subclass this and pass an instance to {@link MagnesiumApplication#run(Application, int)}:
 *
 * <pre>{@code
 * public class MainApplication extends Application {
 *
 *     @Override
 *     protected void configure(MagnesiumRuntime runtime) {
 *         runtime.executor(Executors.newVirtualThreadPerTaskExecutor())
 *                .json(new JacksonJsonProvider())
 *                .services(s -> s.register(OrderService.class, ctx -> new OrderService()))
 *                .http(http -> http
 *                    .route(GET, "/health", req -> Response.ok("up"))
 *                    .mount(ctx -> new OrderController(ctx.get(OrderService.class))));
 *     }
 *
 *     @Override
 *     protected void start(StartContext ctx) {
 *         ctx.serviceRegistry().get(OrderService.class).warmUp();
 *     }
 *
 *     @Override
 *     protected void stop(MagnesiumRuntime runtime) {
 *         runtime.serviceRegistry().get(OrderService.class).shutdown();
 *     }
 * }
 *
 * // Entry point:
 * MagnesiumApplication.run(new MainApplication(), 8080);
 * }</pre>
 *
 * <h3>Lifecycle order</h3>
 * <ol>
 *   <li>{@link #configure(MagnesiumRuntime)} — pure, synchronous configuration; no I/O.</li>
 *   <li>Runtime is frozen, bootstrap runs, server binds.</li>
 *   <li>{@link #start(MagnesiumRuntime)}, just before the server starts; warm-up, cache priming, etc.</li>
 *   <li><em>Application runs …</em></li>
 *   <li>{@link #ready(MagnesiumRuntime, int)}, after the server starts but just before the main thread is donated to the transport's worker pool or is (more commonly) blocked.</li>
 *   <li>{@link #stop(MagnesiumRuntime)} — JVM shutdown signal received; release resources.</li>
 * </ol>
 */
public abstract class Application {

    /**
     * Pure configuration phase — called before any server thread starts.
     *
     * <p>Register services, routes, exception handlers, and all other wiring here.
     * This method must be synchronous and side-effect-free with respect to I/O.
     * The {@code runtime} reference must not be stored; it will be frozen immediately
     * after this method returns.
     *
     * @param runtime mutable runtime handle; frozen after this method returns
     */
    public abstract void configure(MagnesiumRuntime runtime);

    /**
     * Startup hook, called after the server has successfully bound its port and
     * is ready to accept requests.
     *
     * <p>Use this for warm-up work: priming caches, validating external dependencies,
     * scheduling background jobs. Throwing from this method will abort startup.
     *
     * @param runtime frozen runtime; all services are still reachable
     */
    public void start(MagnesiumRuntime runtime) throws Exception {}

    /**
     * Post-bind hook — called after the HTTP server has successfully bound its port
     * and is actively accepting connections, but <em>before</em> the main thread
     * is donated to the transport's worker pool.
     *
     * <p>This is the earliest point at which the server address is guaranteed to be
     * reachable. Use it for:
     * <ul>
     *   <li>registering with a service-discovery system (Consul, Eureka, etc.)</li>
     *   <li>starting background jobs or schedulers that may call back into the server</li>
     *   <li>emitting a "server ready" health signal to an orchestrator</li>
     * </ul>
     *
     * <p>Throwing from this method will trigger a graceful shutdown.
     *
     * @param runtime frozen runtime; all services are reachable
     * @param port    the actual port the server bound to (useful when port 0 was requested)
     */
    public void ready(MagnesiumRuntime runtime, int port) throws Exception {}

    /**
     * Shutdown hook, called on JVM shutdown signal (SIGTERM / SIGINT) before
     * the process exits. Not called on hard kills ({@code SIGKILL}).
     *
     * <p>Use this to drain in-flight work, close connections, flush buffers, etc.
     * The frozen runtime is provided for read-only access to registered services.
     *
     * @param runtime frozen runtime; all services are still reachable
     */
    public void stop(MagnesiumRuntime runtime) throws Exception {}
}