package net.magnesiumbackend.core.event;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * A {@link ScheduledExecutorService} that separates scheduling from execution.
 *
 * <p>Ticks are scheduled on {@code scheduler} (a single platform thread), but the
 * actual {@link Runnable} is handed off to {@code worker} for execution. This lets
 * {@code VIRTUAL_THREAD} tasks schedule precisely while running on virtual threads.
 *
 * <p>This class only implements the subset of the interface that {@link Task} uses.
 * Bulk methods ({@code invokeAll}, {@code invokeAny}) throw {@link UnsupportedOperationException}.
 */
final class DelegatingScheduledExecutor extends AbstractExecutorService
        implements ScheduledExecutorService {

    private final ScheduledExecutorService scheduler;
    private final ExecutorService worker;

    DelegatingScheduledExecutor(ScheduledExecutorService scheduler, ExecutorService worker) {
        this.scheduler = scheduler;
        this.worker    = worker;
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(
            Runnable command, long initialDelay, long period, TimeUnit unit) {
        return scheduler.scheduleAtFixedRate(
            () -> worker.execute(command), initialDelay, period, unit
        );
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(
            Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return scheduler.scheduleWithFixedDelay(
            () -> worker.execute(command), initialDelay, delay, unit
        );
    }

    @Override
    public void shutdown() {
        scheduler.shutdown();
        worker.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        List<Runnable> pending = scheduler.shutdownNow();
        pending.addAll(worker.shutdownNow());
        return pending;
    }

    @Override
    public boolean isShutdown() {
        return scheduler.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return scheduler.isTerminated() && worker.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long deadlineNs = System.nanoTime() + unit.toNanos(timeout);
        if (!scheduler.awaitTermination(timeout, unit)) return false;
        long remainingNs = deadlineNs - System.nanoTime();
        return worker.awaitTermination(remainingNs, TimeUnit.NANOSECONDS);
    }

    @Override
    public void execute(Runnable command) {
        worker.execute(command);
    }

    @Override public ScheduledFuture<?> schedule(Runnable c, long d, TimeUnit u) {
        throw new UnsupportedOperationException();
    }
    @Override public <V> ScheduledFuture<V> schedule(Callable<V> c, long d, TimeUnit u) {
        throw new UnsupportedOperationException();
    }
    @Override public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) {
        throw new UnsupportedOperationException();
    }
    @Override public <T> T invokeAny(Collection<? extends Callable<T>> tasks) {
        throw new UnsupportedOperationException();
    }
}