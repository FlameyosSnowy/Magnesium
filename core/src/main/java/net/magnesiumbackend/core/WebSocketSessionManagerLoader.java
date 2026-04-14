package net.magnesiumbackend.core;

import net.magnesiumbackend.core.http.websocket.WebSocketSessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Loads the WebSocket session manager implementation using Java's {@link ServiceLoader}.
 *
 * <p>WebSocket session managers handle the lifecycle of WebSocket connections,
 * including session creation, message routing, and cleanup. They are provided
 * by transport modules (e.g., Netty, Tomcat) and discovered at runtime.</p>
 *
 * <p>Only one session manager may be present on the classpath. Having multiple
 * managers results in an {@link IllegalStateException}.</p>
 *
 * <p>The session manager is loaded during {@link MagnesiumApplication} initialization
 * if WebSocket routes are configured.</p>
 *
 * @see WebSocketSessionManager
 * @see MagnesiumApplication
 * @see ServiceLoader
 */
public final class WebSocketSessionManagerLoader {

    private WebSocketSessionManagerLoader() {
    }

    /**
     * Discovers and loads the single WebSocketSessionManager implementation.
     *
     * <p>Returns {@link Optional#empty()} if no session manager is found on the classpath.
     * Throws {@link IllegalStateException} if multiple managers are found.</p>
     *
     * @return the loaded session manager, or empty if none found
     * @throws IllegalStateException if more than one session manager is available
     */
    public static Optional<WebSocketSessionManager> load() {
        List<ServiceLoader.Provider<WebSocketSessionManager>> providers =
            ServiceLoader.load(WebSocketSessionManager.class)
                .stream()
                .toList();

        if (providers.size() > 1) {
            List<String> list = new ArrayList<>(providers.size());
            for (ServiceLoader.Provider<WebSocketSessionManager> p : providers) {
                String name = p.type().getName();
                list.add(name);
            }
            throw new IllegalStateException(
                "More than one WebSocketSessionManager found on classpath. " +
                    "Add only one transport dependency. " +
                    "Found: " + list
            );
        }

        ServiceLoader.Provider<WebSocketSessionManager> first = providers.getFirst();
        return first != null ? Optional.ofNullable(first.get()) : Optional.empty();
    }
}