package net.magnesiumbackend.core.event;

import org.jetbrains.annotations.NotNull;

/**
 * Thin facade that receives events published by {@code @Emit}-annotated methods
 * and forwards them to the {@link SubscribeRegistry}.
 *
 * <h2>Why a separate services?</h2>
 * {@code @Emit} is the <em>producer</em> side of the event bus; it only needs to
 * publish.  Keeping it separate from {@link SubscribeRegistry} lets the processor
 * generate compact publish-only wrappers without pulling in dispatch logic,
 * and allows the two sides to be wired independently (e.g. swapped in tests).
 *
 * <h2>Generated usage</h2>
 * The annotation processor wraps every {@code @Emit} method so that its return
 * value (a {@link Event} subclass) is handed to
 * {@link #publish(Event)} immediately after the original method body
 * returns:
 *
 * <pre>{@code
 * // Original:
 * @Emit
 * public UserCreatedEvent createUser(String name) { ... return new UserCreatedEvent(name); }
 *
 * // Generated wrapper in the service proxy:
 * public UserCreatedEvent createUser(String name) {
 *     UserCreatedEvent __event = __delegate.createUser(name);
 *     __emitRegistry.publish(__event);
 *     return __event;
 * }
 * }</pre>
 */
public record EmitRegistry(SubscribeRegistry subscribeRegistry) {
    /**
     * Publishes {@code event} to all registered subscribers.
     * Dispatch follows the executor configured on the underlying
     * {@link SubscribeRegistry} (async by default).
     */
    public <E extends Event<ID>, ID> void publish(@NotNull E event) {
        subscribeRegistry.dispatch(event);
    }

    /**
     * Publishes {@code event} synchronously on the calling thread,
     * bypassing the executor.  Useful when the caller needs to inspect
     * mutations made by subscribers (e.g. cancellation) before continuing.
     */
    public <E extends Event<ID>, ID> void publishSync(@NotNull E event) {
        subscribeRegistry.dispatchSync(event);
    }

    /**
     * Returns the underlying {@link SubscribeRegistry}.
     */
    @Override
    public SubscribeRegistry subscribeRegistry() {
        return subscribeRegistry;
    }
}