package net.magnesiumbackend.actuator.health;

import net.magnesiumbackend.core.health.Health;
import net.magnesiumbackend.core.health.HealthIndicator;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;

/**
 * Health indicator for disk space.
 *
 * <p>Reports DOWN if available disk space falls below a threshold.</p>
 *
 * <h3>Configuration</h3>
 * <pre>{@code
 * // Default threshold: 10MB
 * DiskSpaceHealthIndicator indicator = new DiskSpaceHealthIndicator();
 *
 * // Custom threshold
 * DiskSpaceHealthIndicator indicator = new DiskSpaceHealthIndicator(
 *     Path.of("/data"), 100 * 1024 * 1024L); // 100MB
 * }</pre>
 */
public final class DiskSpaceHealthIndicator implements HealthIndicator {

    private final Path path;
    private final long thresholdBytes;

    /**
     * Creates an indicator for the current working directory with 10MB threshold.
     */
    public DiskSpaceHealthIndicator() {
        this(Path.of("."), 10 * 1024 * 1024L); // 10MB default
    }

    /**
     * Creates an indicator with custom path and threshold.
     *
     * @param path the path to check
     * @param thresholdBytes minimum free space required (in bytes)
     */
    public DiskSpaceHealthIndicator(@NotNull Path path, long thresholdBytes) {
        this.path = path.toAbsolutePath().normalize();
        this.thresholdBytes = thresholdBytes;
    }

    @Override
    public @NotNull Health health() {
        File file = path.toFile();

        if (!file.exists()) {
            return Health.down()
                .withDetail("path", path.toString())
                .withDetail("error", "Path does not exist")
                .build();
        }

        long totalSpace = file.getTotalSpace();
        long freeSpace = file.getFreeSpace();
        long usableSpace = file.getUsableSpace();

        Health.Builder builder = Health.up()
            .withDetail("path", path.toString())
            .withDetail("total", formatBytes(totalSpace))
            .withDetail("free", formatBytes(freeSpace))
            .withDetail("usable", formatBytes(usableSpace))
            .withDetail("threshold", formatBytes(thresholdBytes));

        if (usableSpace < thresholdBytes) {
            return Health.down()
                .withDetail("path", path.toString())
                .withDetail("total", formatBytes(totalSpace))
                .withDetail("free", formatBytes(freeSpace))
                .withDetail("usable", formatBytes(usableSpace))
                .withDetail("threshold", formatBytes(thresholdBytes))
                .withDetail("error", "Free disk space below threshold")
                .build();
        }

        // Calculate percentage
        double freePercent = totalSpace > 0 ? (100.0 * freeSpace / totalSpace) : 0;
        builder.withDetail("freePercent", String.format("%.2f%%", freePercent));

        return builder.build();
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    @Override
    public @NotNull String name() {
        return "diskSpace";
    }
}
