package net.magnesiumbackend.core.registry;

import net.magnesiumbackend.core.annotations.enums.EventPriority;
import net.magnesiumbackend.core.event.Cancellable;
import net.magnesiumbackend.core.event.Event;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Stores and dispatches {@code @Subscribe}-annotated listeners.
 *
 * <h2>Storage model</h2>
 * Listeners are resolved eagerly at startup (the generated registration class
 * instantiates the enclosing listener class and wraps each method in a
 * {@link ListenerEntry}).
 *
 * <h2>Dispatch model</h2>
 * By default, events are dispatched asynchronously on the configured
 * {@link Executor} (defaults to {@link ForkJoinPool#commonPool()}).
 * Call {@link #setExecutor(Executor)} to change the executor, or
 * {@link #dispatchSync(net.magnesiumbackend.core.event.Event)} to fire synchronously on the
 * calling thread regardless of the configured executor.
 *
 * <h2>Priority &amp; cancellation</h2>
 * Listeners are sorted by {@link EventPriority} descending (HIGHEST first).
 * A listener whose {@code ignoresCancelled = false} (the default) will be
 * skipped if the event has already been canceled by an earlier listener.
 * A listener whose {@code ignoresCancelled = true} always runs.
 */
public final class SubscribeRegistry {

    /**
     * A single resolved listener ready to be invoked without reflection.
     *
     * @param priority          dispatch priority, higher enum ordinal fires first
     * @param ignoresCancelled  if {@code true}, the listener runs even when the
     *                          event has been canceled
     * @param handler           the actual invocation target (generated lambda)
     * @param <E>               the event type
     */
    public record ListenerEntry<E extends Event<ID>, ID>(
        EventPriority priority,
        boolean       ignoresCancelled,
        Handler<E, ID>    handler
    ) {}

    /** Functional interface for a listener body. */
    @FunctionalInterface
    public interface Handler<E extends Event<ID>, ID> {
        void handle(E event);
    }

    /** event type → sorted list of entries (sorted on first access, then cached) */
    private final Map<Class<? extends Event<?>>, List<ListenerEntry<?, ?>>> listeners =
        new ConcurrentHashMap<>();

    private Executor executor = ForkJoinPool.commonPool();

    /**
     * Registers a listener entry for the given event type.
     * The list is re-sorted after each registration so that the order is
     * always correct even when registrations happen in arbitrary order.
     *
     * <p>This method is intended to be called only from generated
     * {@code GeneratedSubscriberClass} implementations.
     */
    public <E extends Event<ID>, ID> void register(
        @NotNull Class<E>          eventType,
        @NotNull EventPriority     priority,
        boolean                    ignoresCancelled,
        @NotNull Handler<E, ID>        handler
    ) {
        List<ListenerEntry<?, ?>> bucket = listeners.computeIfAbsent(
            eventType, k -> new ArrayList<>());

        bucket.add(new ListenerEntry<>(priority, ignoresCancelled, handler));

        // Keep sorted: HIGHEST ordinal first
        bucket.sort(Comparator.comparingInt(
            (ListenerEntry<?, ?> e) -> e.priority().ordinal()).reversed());
    }

    /**
     * Replaces the executor used for async dispatch.
     * Pass {@code Runnable::run} to make every dispatch synchronous by default.
     */
    public void setExecutor(@NotNull Executor executor) {
        this.executor = executor;
    }

    /**
     * Dispatches {@code event} asynchronously on the configured executor.
     * Returns immediately; listener exceptions are caught and printed to stderr
     * (override {@link #onListenerException} to customise).
     */
    public <E extends Event<ID>, ID> void dispatch(@NotNull E event) {
        executor.execute(() -> dispatchSync(event));
    }

    /**
     * Dispatches {@code event} synchronously on the calling thread.
     * Listener exceptions are forwarded to {@link #onListenerException}.
     */
    @SuppressWarnings("unchecked")
    public <E extends Event<ID>, ID> void dispatchSync(@NotNull E event) {
        List<ListenerEntry<?, ?>> bucket = listeners.get(event.getClass());
        if (bucket == null || bucket.isEmpty()) return;

        for (ListenerEntry<?, ?> raw : bucket) {
            ListenerEntry<E, ID> entry = (ListenerEntry<E, ID>) raw;

            // Skip canceled events for listeners that care about cancellation
            if (event instanceof Cancellable cancellable && cancellable.isCancelled() && !entry.ignoresCancelled()) {
                continue;
            }

            try {
                entry.handler().handle(event);
            } catch (Exception ex) {
                onListenerException(event, entry, ex);
            }
        }
    }

    /**
     * Called when a listener throws an unchecked exception during dispatch.
     * The default implementation prints the stack trace.  Override in a subclass
     * or replace the registry to route errors elsewhere.
     */
    protected <ID> void onListenerException(
        Event<ID> event,
        ListenerEntry<?, ID> entry,
        Exception exception
    ) {
        System.err.printf(
            "[SubscribeRegistry] Listener (priority=%s, ignoresCancelled=%b) " +
            "threw an exception while handling %s:%n",
            entry.priority(), entry.ignoresCancelled(),
            event.getClass().getSimpleName());
        exception.printStackTrace(System.err);
    }
}