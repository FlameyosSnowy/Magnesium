package net.magnesiumbackend.core.event;

/**
 * Marker interface that signals an event can be cancelled.
 *
 * <p>When a listener with {@code @Subscribe(ignoresCancelled = false)} (the default)
 * receives an event whose {@link Cancellable#isCancelled()} returns {@code true},
 * it is skipped. A listener with {@code ignoresCancelled = true} is always invoked
 * regardless of cancellation state.
 */
public interface Cancellable {
    boolean isCancelled();

    void setCancelled(boolean cancelled);
}
