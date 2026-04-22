package net.magnesiumbackend.actuator.health;

import net.magnesiumbackend.core.health.Health;
import net.magnesiumbackend.core.health.HealthIndicator;
import net.magnesiumbackend.core.health.HealthStatus;
import org.jetbrains.annotations.NotNull;

import java.lang.management.*;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Health indicator for JVM metrics.
 *
 * <p>Reports JVM memory, threads, and uptime information.</p>
 */
public final class JvmHealthIndicator implements HealthIndicator {

    private static final long WARNING_MEMORY_THRESHOLD = 90; // 90% heap usage

    @Override
    public @NotNull Health health() {
        Runtime runtime = Runtime.getRuntime();

        // Memory
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();

        long heapUsed = heapUsage.getUsed();
        long heapMax = heapUsage.getMax();
        long heapCommitted = heapUsage.getCommitted();

        double heapUsedPercent = heapMax > 0 ? (100.0 * heapUsed / heapMax) : 0;

        // Threads
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        int threadCount = threadMXBean.getThreadCount();
        int peakThreadCount = threadMXBean.getPeakThreadCount();

        // Uptime
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        long uptimeMillis = runtimeMXBean.getUptime();
        Duration uptime = Duration.ofMillis(uptimeMillis);

        // GC
        List<GarbageCollectorMXBean> gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
        long gcCollectionCount = gcMXBeans.stream()
            .mapToLong(GarbageCollectorMXBean::getCollectionCount)
            .sum();
        long gcCollectionTime = gcMXBeans.stream()
            .mapToLong(GarbageCollectorMXBean::getCollectionTime)
            .sum();

        Health.Builder builder = Health.up()
            .withDetail("uptime", formatDuration(uptime))
            .withDetail("uptimeMs", uptimeMillis)
            .withDetail("startTime", Instant.ofEpochMilli(runtimeMXBean.getStartTime()).toString())
            .withDetail("memory", Map.of(
                "heap", Map.of(
                    "used", heapUsed,
                    "committed", heapCommitted,
                    "max", heapMax,
                    "usedPercent", String.format("%.2f%%", heapUsedPercent)
                ),
                "nonHeap", Map.of(
                    "used", nonHeapUsage.getUsed(),
                    "committed", nonHeapUsage.getCommitted()
                )
            ))
            .withDetail("threads", Map.of(
                "active", threadCount,
                "peak", peakThreadCount
            ))
            .withDetail("gc", Map.of(
                "collectionCount", gcCollectionCount,
                "collectionTimeMs", gcCollectionTime
            ))
            .withDetail("availableProcessors", runtime.availableProcessors());

        // Warn if memory usage is high
        if (heapUsedPercent > WARNING_MEMORY_THRESHOLD) {
            return Health.status(HealthStatus.OUT_OF_SERVICE)
                .withDetail("uptime", formatDuration(uptime))
                .withDetail("warning", "Heap memory usage exceeds " + WARNING_MEMORY_THRESHOLD + "%")
                .withDetail("memory", Map.of(
                    "heap", Map.of(
                        "used", heapUsed,
                        "committed", heapCommitted,
                        "max", heapMax,
                        "usedPercent", String.format("%.2f%%", heapUsedPercent)
                    )
                ))
                .build();
        }

        return builder.build();
    }

    private String formatDuration(Duration duration) {
        long seconds = duration.getSeconds();
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (days > 0) {
            return String.format("%dd %dh %dm %ds", days, hours, minutes, secs);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, secs);
        } else {
            return String.format("%ds", secs);
        }
    }

    @Override
    public @NotNull String name() {
        return "jvm";
    }
}
