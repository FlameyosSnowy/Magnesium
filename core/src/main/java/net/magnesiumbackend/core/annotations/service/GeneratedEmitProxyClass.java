package net.magnesiumbackend.core.annotations.service;

import net.magnesiumbackend.core.MagnesiumApplication;
import net.magnesiumbackend.core.event.EmitRegistry;
import net.magnesiumbackend.core.services.ServiceRegistry;

/**
 * Implemented by every proxy class the annotation processor generates for a
 * type containing {@code @Emit}-annotated methods.
 *
 * <p>The generated class is named {@code <OriginalClass>_magnesium_EmitProxy}
 * and placed in the same package.  It wraps the original class instance and
 * overrides each {@code @Emit} method so that the return value is automatically
 * published to the {@link EmitRegistry} after the original body runs.
 *
 * <h2>Lifecycle</h2>
 * {@link #create} is called once at startup.  The returned proxy should be
 * stored in {@link ServiceRegistry} so that injection sites receive the
 * event-aware version.
 */
public interface GeneratedEmitProxyClass {
    /**
     * The original service type this proxy wraps.
     * Used to re-register the proxy under the same key in {@link ServiceRegistry}.
     */
    Class<?> serviceType();

    /**
     * Creates the emit proxy, wiring together the original service instance
     * (resolved from {@code serviceRegistry}) and the {@code emitRegistry}.
     *
     * @param application     the running application
     * @param serviceRegistry used to resolve the wrapped service instance
     * @param emitRegistry    receives events published by {@code @Emit} methods
     * @return the proxy instance; callers should treat it as the original type
     */
    Object create(
        MagnesiumApplication application,
        ServiceRegistry      serviceRegistry,
        EmitRegistry         emitRegistry
    );
}