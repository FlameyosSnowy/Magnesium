package net.magnesiumbackend.core.json;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Service loader for {@link JsonProvider} implementations.
 *
 * <p>Follows the same pattern as {@link net.magnesiumbackend.core.TransportLoader}.
 * Loads JsonProvider implementations from the classpath using {@link ServiceLoader}.
 *
 * <p>Only one JsonProvider is expected on the classpath. If multiple are found,
 * an {@link IllegalStateException} is thrown.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * JsonProvider provider = JsonProviderLoader.load()
 *     .orElseThrow(() -> new IllegalStateException("No JSON provider found"));
 * }</pre>
 */
public final class JsonProviderLoader {

    private JsonProviderLoader() {}

    /**
     * Loads a JsonProvider from the service registry.
     *
     * @return the loaded provider, or empty if none found
     * @throws IllegalStateException if more than one provider is found
     */
    public static Optional<JsonProvider> load() {
        List<JsonProvider> providers = loadAll();

        if (providers.isEmpty()) {
            return Optional.empty();
        }

        if (providers.size() > 1) {
            List<String> list = new ArrayList<>(providers.size());
            for (JsonProvider p : providers) {
                String name = p.getClass().getName();
                list.add(name);
            }
            throw new IllegalStateException(
                "More than one JsonProvider found on classpath. " +
                    "Add only one JSON library dependency. " +
                    "Found: " + list
            );
        }

        JsonProvider first = providers.getFirst();
        return Optional.ofNullable(first);
    }

    /**
     * Loads all JsonProvider implementations from the service registry.
     *
     * @return list of all available providers
     */
    public static List<JsonProvider> loadAll() {
        ServiceLoader<JsonProvider> loader = ServiceLoader.load(JsonProvider.class);

        List<JsonProvider> providers = new ArrayList<>(16);
        for (JsonProvider provider : loader) {
            providers.add(provider);
        }
        return providers;
    }
}
