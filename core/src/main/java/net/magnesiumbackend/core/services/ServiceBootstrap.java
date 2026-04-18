package net.magnesiumbackend.core.services;

import net.magnesiumbackend.core.MagnesiumRuntime;
import net.magnesiumbackend.core.annotations.service.GeneratedServiceClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Bootstrap for Magnesium services using compile-time generated wiring.
 *
 * <p>This class uses Java's {@link ServiceLoader} mechanism to discover
 * all {@link GeneratedServiceClass} implementations that were generated at compile time
 * for classes annotated with {@code @RestService}.
 *
 * <h3>Generated Components</h3>
 * <p>For each {@code @RestService} class, the annotation processor generates:
 * <ul>
 *   <li>A {@code __ServiceRegistration} class implementing {@link GeneratedServiceClass}</li>
 *   <li>A {@code META-INF/services/net.magnesiumbackend.core.annotations.service.GeneratedServiceClass} entry</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * public class MyApplication extends Application {
 *     @Override
 *     public void configure(MagnesiumRuntime runtime) {
 *         runtime.services(services -> ServiceBootstrap.wireAll(runtime, services));
 *     }
 * }
 * }</pre>
 *
 * <p><strong>Note:</strong> Services are also automatically loaded by
 * {@link net.magnesiumbackend.core.MagnesiumBootstrap}. This method is provided
 * for explicit control or additional service registration phases.</p>
 *
 * @see GeneratedServiceClass
 * @see java.util.ServiceLoader
 */
public class ServiceBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceBootstrap.class);

    private ServiceBootstrap() {}

    /**
     * Wires all discovered services into the given registry.
     *
     * <p>Uses ServiceLoader to find all generated GeneratedServiceClass implementations,
     * sorts them by lifecycle priority, then calls their register() method.
     *
     * <p>Zero reflection - all service instantiation is compile-time generated.
     *
     * @param runtime  the application runtime
     * @param registry the service registry to register with
     */
    public static void wireAll(MagnesiumRuntime runtime, ServiceRegistrar registry) {
        ServiceLoader<GeneratedServiceClass> loader = ServiceLoader.load(GeneratedServiceClass.class);

        List<GeneratedServiceClass> services = loader.stream()
            .map(ServiceLoader.Provider::get)
            .sorted(Comparator.comparingInt(GeneratedServiceClass::priority))
            .toList();

        for (GeneratedServiceClass registration : services) {
            registration.register(runtime, registry);

            LOGGER.debug("Registered service: {}",
                registration.serviceType().getSimpleName());
        }

        LOGGER.info("Auto-wired {} services", services.size());
    }

    /**
     * Exception thrown during service bootstrapping.
     */
    public static class ServiceBootstrapException extends RuntimeException {
        public ServiceBootstrapException(String message) {
            super(message);
        }

        public ServiceBootstrapException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
