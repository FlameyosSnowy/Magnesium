package net.magnesiumbackend.core.annotations.service;

import net.magnesiumbackend.core.MagnesiumApplication;
import net.magnesiumbackend.core.services.ServiceRegistry;
import net.magnesiumbackend.core.event.SubscribeRegistry;

/**
 * Implemented by every class the annotation processor generates for a
 * {@code @Subscribe}-containing type.
 *
 * <p>The generated class is named {@code <OriginalClass>_magnesium_Subscriber}
 * and placed in the same package.  Its {@link #register} method instantiates
 * the original class (resolving constructor dependencies from
 * {@link ServiceRegistry}) and registers all {@code @Subscribe} methods into
 * the provided {@link SubscribeRegistry}.
 */
public interface GeneratedSubscriberClass {

    /**
     * Instantiates the listener class and registers all its
     * {@code @Subscribe}-annotated methods.
     *
     * @param application       the running application
     * @param serviceRegistry   used to resolve constructor dependencies
     * @param subscribeRegistry destination services for the listener entries
     */
    void register(
        MagnesiumApplication application,
        ServiceRegistry      serviceRegistry,
        SubscribeRegistry    subscribeRegistry
    );
}