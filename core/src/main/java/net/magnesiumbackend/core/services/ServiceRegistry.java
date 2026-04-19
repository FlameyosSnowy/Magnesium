package net.magnesiumbackend.core.services;

import net.magnesiumbackend.core.config.MagnesiumConfigurationManager;
import net.magnesiumbackend.core.event.EventBus;
import net.magnesiumbackend.core.json.JsonProvider;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Holds service instances, initialised <em>lazily</em> on first
 * {@link #get(Class)} call and cached for all subsequent calls.
 *
 * <h2>Initialization strategy, lazy with circular-dependency detection</h2>
 * Lazy init was chosen over eager init so that services with expensive
 * constructors are only paid for if they are actually used, and so that
 * the startup order of {@code MagnesiumApplication} does not need to
 * topologically sort the dependency graph.
 *
 * <p>Circular dependencies are detected at runtime via an
 * {@code inProgress} set: if {@code get(A)} triggers {@code get(B)} which
 * triggers {@code get(A)} again before A's factory returns, an
 * {@link IllegalStateException} is thrown immediately with a clear message
 * rather than overflowing the stack.
 *
 * <h2>Thread safety</h2>
 * This implementation is <em>not</em> thread-safe.  {@code get} is expected
 * to be called from a single thread during the startup phase.  After startup
 * the map is effectively read-only, so concurrent reads from request threads
 * are safe without synchronization.
 */
public final class ServiceRegistry implements ServiceContext {

    private final Map<Class<?>, Function<ServiceContext, ?>> factories;
    private final EventBus                                   bus;
    private final Map<Class<?>, Object>                      instances   = new HashMap<>(8);
    private final Set<Class<?>>                              inProgress  = new HashSet<>(8);
    private final JsonProvider jsonProvider;
    private final MagnesiumConfigurationManager configurationManager;

    public ServiceRegistry(
        Map<Class<?>, Function<ServiceContext, ?>> factories,
        EventBus bus,
        JsonProvider jsonProvider,
        MagnesiumConfigurationManager configurationManager
    ) {
        this.factories = factories;
        this.bus       = bus;
        this.jsonProvider = jsonProvider;
        this.configurationManager = configurationManager;
    }

    public static ServiceRegistry from(ServiceRegistrar registrar, EventBus eventBus, JsonProvider jsonProvider, MagnesiumConfigurationManager configurationManager) {
        return new ServiceRegistry(registrar.factories(), eventBus, jsonProvider, configurationManager);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> type) {
        Object cached = instances.get(type);
        if (cached != null) return (T) cached;

        if (inProgress.contains(type)) {
            throw new IllegalStateException(
                "Circular dependency detected while constructing " + type.getName() + ". "
                    + "Currently in progress: " + inProgress);
        }

        Function<ServiceContext, ?> factory = factories.get(type);
        if (factory == null) {
            throw new IllegalArgumentException(
                "No factory registered for type: " + type.getName());
        }

        inProgress.add(type);
        try {
            T instance = (T) factory.apply(this);
            if (instance == null) {
                throw new IllegalStateException(
                    "Factory for " + type.getName() + " returned null.");
            }
            instances.put(type, instance);
            return instance;
        } finally {
            inProgress.remove(type);
        }
    }

    public Map<Class<?>, Function<ServiceContext, ?>> factories() {
        return factories;
    }

    @Override
    public JsonProvider jsonProvider() {
        return jsonProvider;
    }

    @Override
    public EventBus eventBus() {
        return bus;
    }

    @Override
    public MagnesiumConfigurationManager configurationManager() {
        return configurationManager;
    }

    /**
     * Replaces (or inserts) a pre-built instance directly, bypassing the factory.
     * Used at startup to swap a plain service instance for its emit proxy.
     *
     * @throws IllegalArgumentException if {@code type} has no registered factory
     *         and no existing instance (i.e. it was never declared as a service)
     */
    public <T> void replaceInstance(Class<T> type, Object instance) {
        if (!factories.containsKey(type) && !instances.containsKey(type)) {
            throw new IllegalArgumentException(
                "Cannot replace instance for unregistered type: " + type.getName());
        }
        instances.put(type, instance);
    }
}