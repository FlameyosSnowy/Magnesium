package net.magnesiumbackend.redis.health;

import net.magnesiumbackend.core.health.Health;
import net.magnesiumbackend.core.health.HealthIndicator;
import net.magnesiumbackend.redis.RedisService;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Health indicator for Redis.
 *
 * <p>Checks Redis connectivity by executing a PING command.</p>
 */
public final class RedisHealthIndicator implements HealthIndicator {

    private final RedisService redisService;
    private final Duration timeout;

    public RedisHealthIndicator(@NotNull RedisService redisService) {
        this(redisService, Duration.ofSeconds(5));
    }

    public RedisHealthIndicator(@NotNull RedisService redisService, @NotNull Duration timeout) {
        this.redisService = redisService;
        this.timeout = timeout;
    }

    @Override
    public @NotNull Health health() {
        try {
            String pong = redisService.withSync(commands -> {
                String info = commands.info("server");
                return info != null ? "PONG" : null;
            });

            if (pong == null) {
                return Health.down()
                    .withDetail("error", "Redis connection failed")
                    .build();
            }

            String redisVersion = redisService.withSync(commands -> commands.info("server"));
            String version = extractVersion(redisVersion);

            return Health.up()
                .withDetail("version", version)
                .withDetail("status", "connected")
                .build();

        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .withException(e)
                .build();
        }
    }

    private String extractVersion(String info) {
        if (info == null || info.isEmpty()) {
            return "unknown";
        }
        for (String line : info.split("\r?\n")) {
            if (line.startsWith("redis_version:")) {
                return line.substring("redis_version:".length()).trim();
            }
        }
        return "unknown";
    }

    @Override
    public @NotNull String name() {
        return "redis";
    }
}
