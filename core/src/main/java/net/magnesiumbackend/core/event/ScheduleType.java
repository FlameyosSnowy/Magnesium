package net.magnesiumbackend.core.event;

import java.util.concurrent.*;

/**
 * Determines which executor backs a {@link Task}.
 *
 * <p>Each type creates its own isolated {@link ScheduledExecutorService} so tasks
 * don't share thread pools and can't starve each other.
 */
public enum ScheduleType {

    PLATFORM_THREAD {
        @Override
        ScheduledExecutorService createExecutor(String taskName) {
            return Executors.newSingleThreadScheduledExecutor(r ->
                Thread.ofPlatform()
                    .name("task-" + taskName)
                    .unstarted(r)
            );
        }
    },

    VIRTUAL_THREAD {
        @Override
        ScheduledExecutorService createExecutor(String taskName) {
            // ScheduledExecutorService doesn't natively support virtual threads,
            // so we use a single platform thread for scheduling and hand off
            // each tick to a virtual thread for actual execution.
            ThreadFactory virtualFactory = Thread.ofVirtual()
                .name("task-" + taskName + "-vt-", 0)
                .factory();

            ExecutorService virtualPool = Executors.newThreadPerTaskExecutor(virtualFactory);

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r ->
                Thread.ofPlatform()
                    .name("task-" + taskName + "-scheduler")
                    .daemon(true)
                    .unstarted(r)
            );

            return new DelegatingScheduledExecutor(scheduler, virtualPool);
        }
    };

    /** Creates the backing executor for a task with the given name. */
    abstract ScheduledExecutorService createExecutor(String taskName);
}