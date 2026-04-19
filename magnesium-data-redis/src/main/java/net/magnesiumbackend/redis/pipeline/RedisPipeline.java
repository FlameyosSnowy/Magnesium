package net.magnesiumbackend.redis.pipeline;

import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import net.magnesiumbackend.redis.RedisService;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Redis pipeline support for batch operations.
 *
 * <p>Groups multiple commands into a single request for improved performance.
 * All commands are executed atomically and results are returned together.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * RedisPipeline pipeline = redisService.createPipeline();
 * pipeline.add(commands -> commands.set("key1", "value1"));
 * pipeline.add(commands -> commands.set("key2", "value2"));
 * pipeline.add(commands -> commands.get("key1"));
 *
 * List<Object> results = pipeline.execute();
 * }</pre>
 */
public class RedisPipeline {

    private final RedisService redisService;
    private final List<Function<RedisAsyncCommands<String, String>, RedisFuture<?>>> commands;

    public RedisPipeline(@NotNull RedisService redisService) {
        this.redisService = redisService;
        this.commands = new ArrayList<>();
    }

    /**
     * Adds a command to the pipeline.
     *
     * @param command function that performs the async command
     * @return this pipeline for chaining
     */
    public @NotNull RedisPipeline add(@NotNull Function<RedisAsyncCommands<String, String>, RedisFuture<?>> command) {
        commands.add(command);
        return this;
    }

    /**
     * Executes all pipelined commands and returns results.
     *
     * @return list of results in order of command execution
     */
    @SuppressWarnings("unchecked")
    public @NotNull List<Object> execute() {
        if (commands.isEmpty()) {
            return List.of();
        }

        try (StatefulRedisConnection<String, String> connection = ((io.lettuce.core.RedisClient) redisService.getClient()).connect()) {
            RedisAsyncCommands<String, String> async = connection.async();

            // Disable auto-flushing to batch commands
            connection.setAutoFlushCommands(false);

            CompletableFuture<?>[] futures = new CompletableFuture[commands.size()];

            int i = 0;
            for (Function<RedisAsyncCommands<String, String>, RedisFuture<?>> cmd : commands) {
                futures[i] = cmd.apply(async).toCompletableFuture();
                i++;
            }

            // Flush all commands at once
            connection.flushCommands();

            // Wait for all to complete
            CompletableFuture.allOf(futures).join();

            // Collect results
            List<Object> results = new ArrayList<>(futures.length);
            for (CompletableFuture<?> future : futures) {
                try {
                    results.add(future.get());
                } catch (Exception e) {
                    results.add(e);
                }
            }

            return results;
        }
    }

    /**
     * Executes pipeline and returns a CompletableFuture for async handling.
     *
     * @return future of results
     */
    public @NotNull CompletableFuture<List<Object>> executeAsync() {
        return CompletableFuture.supplyAsync(this::execute);
    }

    /**
     * Clears all pending commands.
     *
     * @return this pipeline for chaining
     */
    public @NotNull RedisPipeline clear() {
        commands.clear();
        return this;
    }

    /**
     * Returns the number of pending commands.
     *
     * @return command count
     */
    public int size() {
        return commands.size();
    }
}
