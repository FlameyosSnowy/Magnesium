package net.magnesiumbackend.core.event;

import net.magnesiumbackend.core.annotations.enums.EventPriority;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central bus for event publishing/subscribing and task scheduling.
 *
 * <p>Event listeners and scheduled tasks are two sides of the same coin:
 * both are background work dispatched by the framework. Owning both here
 * means a single shutdown call drains everything cleanly.
 *
 * <h2>Events</h2>
 * <pre>{@code
 * eventBus.subscribe(OrderPlaced.class, event -> { ... });
 * eventBus.publish(new OrderPlaced(orderId));
 * }</pre>
 *
 * <h2>Scheduled tasks</h2>
 * <pre>{@code
 * Task task = Task.builder()
 *     .name("sync-inventory")
 *     .schedulerType(ScheduleType.VIRTUAL_THREAD)
 *     .period(5, TimeUnit.MINUTES)
 *     .delay(0, TimeUnit.MILLISECONDS)
 *     .execute((handle) -> { ... })
 *     .build();
 *
 * TaskHandle handle = eventBus.schedule(task);
 * handle.cancel(); // when done
 * }</pre>
 */
public class EventBus {
    private final List<TaskHandle> activeHandles = new CopyOnWriteArrayList<>();

    private final EmitRegistry emitRegistry;
    private final SubscribeRegistry subscribeRegistry = new SubscribeRegistry();
    private final CountDownLatch latch = new CountDownLatch(1);

    public EventBus() {
        this.emitRegistry = new EmitRegistry(subscribeRegistry);
    }

    public void start() {
        this.latch.countDown();
    }

    /**
     * Schedules a {@link Task} and returns a {@link TaskHandle} the caller can
     * use to cancel or inspect it.
     *
     * <p>The executor backing the task is created here and owned by the returned handle.
     * Calling {@link TaskHandle#cancel()} releases that executor.
     *
     * @param task the task descriptor; must not be {@code null}
     * @return a handle to the running task
     */
    public TaskHandle schedule(Task task) {
        ScheduledExecutorService executor = task.schedulerType().createExecutor(task.name());

        // Handle exists first, action can reference it
        TaskHandleImpl handle = new TaskHandleImpl(task.name(), executor, this.latch);

        ScheduledFuture<?> future = executor.scheduleAtFixedRate(
            () -> task.action(handle).run(),
            task.delayMs(),
            task.periodMs(),
            TimeUnit.MILLISECONDS
        );

        handle.setFuture(future);
        activeHandles.add(handle);
        return handle;
    }

    /**
     * Registers a listener for events of type {@code T}.
     * Listeners are called in registration order on the publishing thread.
     *
     * @param eventType the event class to listen for
     * @param listener  the handler
     * @param <E>       the event type
     */
    public <E extends Event<ID>, ID> void subscribe(Class<E> eventType, EventPriority priority, boolean ignoreCancelled, SubscribeRegistry.Handler<E, ID> listener) {
        subscribeRegistry.register(eventType, priority, ignoreCancelled, listener);
    }

    /**
     * Publishes {@code event} to all registered listeners for its type.
     * Listeners are invoked synchronously on the calling thread.
     *
     * @param event the event to publish; must not be {@code null}
     * @param <T>   the event type
     */
    public <T extends Event<ID>, ID> void publish(T event) {
        try {
            if (!this.latch.await(60, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out: Application has not started in 60 seconds.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted waiting for future on the application to start.");
        }

        emitRegistry.publish(event);
    }

    /**
     * Cancels all active task handles and removes all event listeners.
     * Called automatically by {@link net.magnesiumbackend.core.MagnesiumApplication} on exit.
     */
    public void shutdown() {
        activeHandles.forEach(TaskHandle::cancel);
        activeHandles.clear();
    }

    public EmitRegistry emitRegistry() {
        return emitRegistry;
    }

    public SubscribeRegistry subscribeRegistry() {
        return subscribeRegistry;
    }

    private static final class TaskHandleImpl implements TaskHandle {
        private final String name;
        private final ScheduledExecutorService executor;
        private final CountDownLatch eventBusLatch;
        private final CountDownLatch futureLatch = new CountDownLatch(1);
        private volatile ScheduledFuture<?> future;

        private TaskHandleImpl(String name, ScheduledExecutorService executor, CountDownLatch latch) {
            this.name = name;
            this.executor = executor;
            this.eventBusLatch = latch;
        }

        public void setFuture(ScheduledFuture<?> future) {
            if (futureLatch.getCount() != 0) { // already set
                throw new IllegalStateException("Future already set for task: " + name);
            }
            this.future = future;
            futureLatch.countDown();
        }

        private ScheduledFuture<?> awaitFuture() {
            try {
                if (!eventBusLatch.await(60, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out: Application has not started in 60 seconds.");
                }
                if (!futureLatch.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Future never set for task: " + name);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted waiting for future on task: " + name);
            }
            return future;
        }

        @Override
        public boolean isRunning() {
            ScheduledFuture<?> f = awaitFuture();
            return !f.isDone() && !f.isCancelled();
        }

        @Override
        public boolean cancel() {
            return cancel(5, TimeUnit.SECONDS);
        }

        @Override
        public boolean cancel(long timeout, TimeUnit unit) {
            awaitFuture().cancel(false);
            executor.shutdown();
            try {
                return executor.awaitTermination(timeout, unit);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        @Override
        public String name() {
            return name;
        }

        public ScheduledFuture<?> future() {
            return awaitFuture();
        }

        public ScheduledExecutorService executor() {
            return executor;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (TaskHandleImpl) obj;
            return Objects.equals(this.name, that.name) &&
                Objects.equals(this.future, that.future) &&
                Objects.equals(this.executor, that.executor);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, future, executor);
        }

        @Override
        public String toString() {
            return "TaskHandleImpl[" +
                "name=" + name + ", " +
                "future=" + (future == null ? "<pending>" : future) + ", " +
                "executor=" + executor + ']';
        }
    }
}