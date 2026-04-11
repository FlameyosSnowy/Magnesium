package net.magnesiumbackend.core;

import net.magnesiumbackend.core.http.MagnesiumTransport;

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

public final class TransportLoader {

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