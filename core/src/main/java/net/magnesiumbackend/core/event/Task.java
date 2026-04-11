package net.magnesiumbackend.core.event;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Immutable descriptor for a scheduled background task.
 *
 * <p>A {@code Task} carries no executor, no thread, and no lifecycle state.
 * It is a pure configuration object, hand it to {@link net.magnesiumbackend.core.event.EventBus#schedule}
 * to actually run it.
 *
 * <pre>{@code
 * Task task = Task.builder()
 *     .schedulerType(ScheduleType.VIRTUAL_THREAD)
 *     .name("worker")
 *     .period(5, TimeUnit.MINUTES)
 *     .delay(0, TimeUnit.MILLISECONDS)
 *     .execute(() -> { ... })
 *     .build();
 *
 * TaskHandle handle = eventBus.schedule(task);
 * }</pre>
 */
public final class Task {

    private final String               name;
    private final ScheduleType         schedulerType;
    private final long                 periodMs;
    private final long                 delayMs;
    private final Consumer<TaskHandle> action;

    private Task(Builder builder) {
        this.name          = builder.name;
        this.schedulerType = builder.schedulerType;
        this.periodMs      = builder.periodMs;
        this.delayMs       = builder.delayMs;
        this.action        = builder.action;
    }

    public static Builder builder() { return new Builder(); }

    public String name() {
        return name;
    }

    public ScheduleType schedulerType() {
        return schedulerType;
    }

    public long periodMs() {
        return periodMs;
    }

    public long delayMs() {
        return delayMs;
    }

    public Runnable action(TaskHandle handle) {
        return () -> action.accept(handle);
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static final class Builder {

        private String               name;
        private ScheduleType         schedulerType;
        private long                 periodMs = -1;
        private long                 delayMs  = 0;
        private Consumer<TaskHandle> action;

        private Builder() {}

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder schedulerType(ScheduleType type) {
            this.schedulerType = type;
            return this;
        }

        public Builder period(long amount, TimeUnit unit) {
            this.periodMs = unit.toMillis(amount);
            return this;
        }

        public Builder delay(long amount, TimeUnit unit) {
            this.delayMs = unit.toMillis(amount);
            return this;
        }

        /** The work to run on each tick. Idiomatic last call before {@link #build()}. */
        public Builder execute(Consumer<TaskHandle> action) {
            this.action = action;
            return this;
        }

        public Task build() {
            if (name == null || name.isBlank())
                throw new IllegalStateException("Task name must not be blank.");
            if (schedulerType == null)
                throw new IllegalStateException("Task '" + name + "' requires a schedulerType.");
            if (periodMs < 0)
                throw new IllegalStateException("Task '" + name + "' requires a period.");
            if (delayMs < 0)
                throw new IllegalStateException("Task '" + name + "' delay must be non-negative.");
            if (action == null)
                throw new IllegalStateException("Task '" + name + "' requires an action via .execute(...).");
            return new Task(this);
        }
    }
}