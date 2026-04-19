package net.magnesiumbackend.core.config;

/**
 * Interface for configuration sources that support hot-reloading.
 *
 * <p>Implementations of this interface can notify listeners when the
 * underlying configuration has been modified, allowing the
 * {@link MagnesiumConfigurationManager} to clear its cache and pick up
 * new values without an application restart.</p>
 *
 * @see ReloadableTomlConfigSource
 */
public interface ReloadableSource {

    /**
     * Registers a callback to be invoked when the configuration is reloaded.
     *
     * <p>The callback should clear any cached configuration values so that
     * the next access will load fresh values from the source.</p>
     *
     * @param callback the action to perform on reload
     */
    void onReload(Runnable callback);
}
