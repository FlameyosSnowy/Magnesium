package net.magnesiumbackend.core.backpressure;

import net.magnesiumbackend.core.MagnesiumApplication;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Resolves the effective {@link Executor} for a transport, optionally wrapping it in a
 * {@link BoundedBackpressureExecutor} when backpressure has been configured.
 *
 * <p>Every transport should call this once in its {@code bind()} method instead of
 * reaching for {@code application.executor()} directly:
 *
 * <pre>{@code
 * Executor requestExecutor = BackpressureExecutorResolver.resolve(application);
 * }</pre>
 *
 * <p>The returned executor is then passed straight into the pipeline/handler factory
 * (e.g., {@code NettyPipelineFactory}) — no other changes are required inside the
 * transport itself.
 *
 * <h2>Resolution logic</h2>
 * <ol>
 *   <li>If backpressure is not configured → return the raw executor (or
 *       {@link ForkJoinPool#commonPool()} when no executor was supplied).</li>
 *   <li>If backpressure is configured → wrap the resolved executor in a
 *       {@link BoundedBackpressureExecutor}.</li>
 * </ol>
 */
public final class BackpressureExecutorResolver {

    private BackpressureExecutorResolver() {}

    /**
     * Returns the effective executor for the given application.
     *
     * @param application the sealed {@link MagnesiumApplication}
     * @return a plain or bounded executor, never {@code null}
     */
    @NotNull
    public static Executor resolve(@NotNull MagnesiumApplication application) {
        Executor base = baseExecutor(application.executor());
        BackpressureConfig config = application.backpressureConfig();

        if (config == null) {
            return base;
        }

        return new BoundedBackpressureExecutor(base, config);
    }

    /**
     * Returns {@code executor} if non-null, otherwise the common ForkJoinPool —
     * the same fallback used by {@link java.util.concurrent.CompletableFuture}.
     */
    @NotNull
    private static Executor baseExecutor(@Nullable Executor executor) {
        return executor != null ? executor : ForkJoinPool.commonPool();
    }
}