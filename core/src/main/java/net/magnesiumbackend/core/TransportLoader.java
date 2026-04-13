package net.magnesiumbackend.core;

import net.magnesiumbackend.core.http.MagnesiumTransport;

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Loads the HTTP transport implementation using Java's {@link ServiceLoader}.
 *
 * <p>Transport implementations (e.g., Netty, Tomcat) are discovered at runtime
 * from the module path or classpath. Only one transport may be present;
 * having multiple transports results in an {@link IllegalStateException}.</p>
 *
 * <p>Available transport modules:</p>
 * <ul>
 *   <li>{@code magnesium-transport-netty} - High-performance Netty-based transport</li>
 *   <li>{@code magnesium-transport-tomcat} - Embedded Tomcat transport</li>
 * </ul>
 *
 * <p>The transport is loaded during {@link MagnesiumApplication} initialization
 * and bound to the configured port when {@code run()} is called.</p>
 *
 * @see MagnesiumTransport
 * @see MagnesiumApplication
 * @see ServiceLoader
 */
public final class TransportLoader {

    private TransportLoader() {
    }

    /**
     * Discovers and loads the single MagnesiumTransport implementation.
     *
     * <p>Returns {@link Optional#empty()} if no transport is found on the classpath.
     * Throws {@link IllegalStateException} if multiple transports are found.</p>
     *
     * @return the loaded transport, or empty if none found
     * @throws IllegalStateException if more than one transport is available
     */
    public static Optional<MagnesiumTransport> load() {
        List<ServiceLoader.Provider<MagnesiumTransport>> providers =
            ServiceLoader.load(MagnesiumTransport.class)
                .stream()
                .toList();

        if (providers.size() > 1) {
            throw new IllegalStateException(
                "More than one TransportProvider found on classpath. " +
                    "Add only one dependency such as magnesium-transport-netty. " +
                    "Found: " + providers.stream().map(p -> p.type().getName()).toList()
            );
        }

        return providers.stream().findFirst().map(ServiceLoader.Provider::get);
    }
}