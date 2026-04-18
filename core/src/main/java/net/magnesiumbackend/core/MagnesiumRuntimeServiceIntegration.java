package net.magnesiumbackend.core;

import net.magnesiumbackend.core.services.ServiceBootstrap;
import net.magnesiumbackend.core.services.ServiceRegistrar;

/**
 * Integration point for service auto-wiring into MagnesiumRuntime.
 *
 * <p>Provides convenient methods to wire compile-time generated services
 * into the application runtime.
 *
 * <h3>Usage in Application.configure()</h3>
 * <pre>{@code
 * public class MyApplication extends Application {
 *     @Override
 *     public void configure(MagnesiumRuntime runtime) {
 *         // Auto-wire all @RestService classes
 *         runtime.services(ServiceBootstrap::wireAll);
 *
 *         // Or with manual registration mixed in:
 *         runtime.services(services -> {
 *             ServiceBootstrap.wireAll(services);
 *             services.register(CustomService.class, ctx -> new CustomService());
 *         });
 *     }
 * }
 * }</pre>
 *
 * @see ServiceBootstrap
 * @see Application
 * @see MagnesiumRuntime
 */
public final class MagnesiumRuntimeServiceIntegration {

    private MagnesiumRuntimeServiceIntegration() {}

    /**
     * Enables auto-wiring for the given runtime.
     *
     * <p>This is a convenience method equivalent to:
     * <pre>{@code runtime.services(ServiceBootstrap::wireAll); }</pre>
     *
     * @param runtime the runtime to enable auto-wiring on
     */
    public static void enableAutoWire(MagnesiumRuntime runtime) {
        runtime.services(services -> ServiceBootstrap.wireAll(runtime, services));
    }
}
