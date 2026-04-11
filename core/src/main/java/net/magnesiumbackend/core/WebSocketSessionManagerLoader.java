package net.magnesiumbackend.core;

import net.magnesiumbackend.core.http.MagnesiumTransport;
import net.magnesiumbackend.core.http.socket.WebSocketSessionManager;

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

public final class WebSocketSessionManagerLoader {

    public static Optional<WebSocketSessionManager> load() {
        List<ServiceLoader.Provider<WebSocketSessionManager>> providers =
            ServiceLoader.load(WebSocketSessionManager.class)
                .stream()
                .toList();

        if (providers.size() > 1) {
            throw new IllegalStateException(
                "More than one TransportProvider found on classpath. " +
                    "Add only one dependency such as magnesium-transport-netty. " +
                    "Found: " + providers.stream().map(p -> p.type().getName()).toList()
            );
        }

        Optional<WebSocketSessionManager> webSocketSessionManager = providers.stream().findFirst().map(ServiceLoader.Provider::get);
        System.out.println("Loaded WebSocketSessionManager: " + webSocketSessionManager);
        return webSocketSessionManager;
    }
}