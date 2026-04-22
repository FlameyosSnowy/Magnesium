package net.magnesiumbackend.redis;

import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.support.ConnectionPoolSupport;
import net.magnesiumbackend.core.lifecycle.Startable;
import net.magnesiumbackend.core.lifecycle.Stoppable;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Core Redis service managing connections using Lettuce driver.
 *
 * <p>Supports single node, cluster, and sentinel configurations with
 * connection pooling and proper lifecycle management.</p>
 *
 * <p>Implements {@link Startable} and {@link Stoppable} for automatic
 * lifecycle management in Magnesium's dependency graph.</p>
 */
public class RedisService implements Startable, Stoppable {

    private final RedisConfiguration configuration;
    private final ClientResources clientResources;
    private AbstractRedisClient redisClient;
    private GenericObjectPool<StatefulRedisConnection<String, String>> connectionPool;
    private StatefulRedisClusterConnection<String, String> clusterConnection;

    public RedisService(@NotNull RedisConfiguration configuration) {
        this.configuration = configuration;
        this.clientResources = DefaultClientResources.builder()
            .build();
    }

    @Override
    public void start() throws Exception {
        if (configuration.isClusterEnabled()) {
            startClusterMode();
        } else if (configuration.isSentinelEnabled()) {
            startSentinelMode();
        } else {
            startStandaloneMode();
        }
    }

    private void startStandaloneMode() throws Exception {
        RedisURI.Builder uriBuilder = RedisURI.builder()
            .withHost(configuration.getHost())
            .withPort(configuration.getPort())
            .withDatabase(configuration.getDatabase())
            .withTimeout(configuration.getTimeout())
            .withClientName(configuration.getClientName());

        if (!configuration.getPassword().isEmpty()) {
            uriBuilder.withPassword(configuration.getPassword().toCharArray());
        }

        RedisURI redisUri = uriBuilder.build();
        this.redisClient = RedisClient.create(clientResources, redisUri);

        // Configure connection pooling
        GenericObjectPoolConfig<StatefulRedisConnection<String, String>> poolConfig =
            new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(configuration.getPoolMaxActive());
        poolConfig.setMaxIdle(configuration.getPoolMaxIdle());
        poolConfig.setMinIdle(configuration.getPoolMinIdle());
        poolConfig.setMaxWait(configuration.getPoolMaxWait());

        this.connectionPool = ConnectionPoolSupport.createGenericObjectPool(
            () -> ((RedisClient) redisClient).connect(),
            poolConfig
        );
    }

    private void startClusterMode() {
        if (configuration.getClusterNodes() == null || configuration.getClusterNodes().isEmpty()) {
            throw new IllegalStateException("Cluster nodes must be configured when cluster mode is enabled");
        }

        List<RedisURI> clusterUris = parseRedisURIs(configuration.getClusterNodes());

        ClusterClientOptions options = ClusterClientOptions.builder()
            .maxRedirects(configuration.getClusterMaxRedirects())
            .build();

        this.redisClient = RedisClusterClient.create(clientResources, clusterUris);
        ((RedisClusterClient) redisClient).setOptions(options);

        // Get a shared cluster connection (not pooled for cluster mode)
        this.clusterConnection = ((RedisClusterClient) redisClient).connect();
    }

    private void startSentinelMode() {
        if (configuration.getSentinelNodes() == null || configuration.getSentinelNodes().isEmpty()) {
            throw new IllegalStateException("Sentinel nodes must be configured when sentinel mode is enabled");
        }

        List<RedisURI> sentinelUris = parseRedisURIs(configuration.getSentinelNodes());

        RedisURI.Builder uriBuilder = RedisURI.builder()
            .withSentinelMasterId(configuration.getSentinelMaster())
            .withTimeout(configuration.getTimeout());

        if (!configuration.getPassword().isEmpty()) {
            uriBuilder.withPassword(configuration.getPassword().toCharArray());
        }

        for (RedisURI sentinelUri : sentinelUris) {
            uriBuilder.withSentinel(sentinelUri.getHost(), sentinelUri.getPort());
        }

        RedisURI redisUri = uriBuilder.build();
        this.redisClient = RedisClient.create(clientResources, redisUri);

        // For sentinel mode, we typically don't use connection pooling
        // but use the Master's connection directly
        this.connectionPool = null;
    }

    private List<RedisURI> parseRedisURIs(String nodes) {
        List<RedisURI> uris = new ArrayList<>();
        for (String node : nodes.split(",")) {
            String[] parts = node.trim().split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);

            RedisURI.Builder builder = RedisURI.builder()
                .withHost(host)
                .withPort(port)
                .withTimeout(configuration.getTimeout());

            if (!configuration.getPassword().isEmpty()) {
                builder.withPassword(configuration.getPassword().toCharArray());
            }

            uris.add(builder.build());
        }
        return uris;
    }

    @Override
    public void stop() throws Exception {
        if (connectionPool != null) {
            connectionPool.close();
        }

        if (clusterConnection != null) {
            clusterConnection.close();
        }

        if (redisClient != null) {
            redisClient.shutdown();
        }

        if (clientResources != null) {
            clientResources.shutdown();
        }
    }

    /**
     * Gets synchronous cluster commands.
     *
     * @return cluster-specific synchronous commands
     */
    public @NotNull RedisAdvancedClusterCommands<String, String> syncCluster() {
        if (!configuration.isClusterEnabled()) {
            throw new IllegalStateException("Not running in cluster mode");
        }
        return clusterConnection.sync();
    }

    /**
     * Gets asynchronous cluster commands.
     *
     * @return cluster-specific asynchronous commands
     */
    public @NotNull RedisAdvancedClusterAsyncCommands<String, String> asyncCluster() {
        if (!configuration.isClusterEnabled()) {
            throw new IllegalStateException("Not running in cluster mode");
        }
        return clusterConnection.async();
    }

    /**
     * Returns the underlying Redis client.
     *
     * @return the Redis client
     */
    public @NotNull AbstractRedisClient getClient() {
        return redisClient;
    }

    public boolean isConnected() {
        try {
            if (configuration.isClusterEnabled()) {
                return clusterConnection != null && clusterConnection.isOpen();
            }

            if (connectionPool != null) {
                try (var conn = connectionPool.borrowObject()) {
                    return conn.isOpen();
                }
            }

            // sentinel fallback
            try (var conn = ((RedisClient) redisClient).connect()) {
                return conn.isOpen();
            }

        } catch (Exception e) {
            return false;
        }
    }

    public <T> T withSync(Function<RedisCommands<String, String>, T> fn) {
        try {
            StatefulRedisConnection<String, String> conn =
                connectionPool != null
                    ? connectionPool.borrowObject()
                    : ((RedisClient) redisClient).connect();

            try {
                return fn.apply(conn.sync());
            } finally {
                if (connectionPool != null) {
                    connectionPool.returnObject(conn);
                } else {
                    conn.close();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> CompletableFuture<T> withAsync(
        Function<RedisAsyncCommands<String, String>, RedisFuture<T>> fn
    ) {
        try {
            StatefulRedisConnection<String, String> conn =
                connectionPool != null
                    ? connectionPool.borrowObject()
                    : ((RedisClient) redisClient).connect();

            try {
                RedisFuture<T> redisFuture = fn.apply(conn.async());

                CompletableFuture<T> future = new CompletableFuture<>();

                redisFuture.whenComplete((result, error) -> {
                    try {
                        if (error != null) {
                            future.completeExceptionally(error);
                        } else {
                            future.complete(result);
                        }
                    } finally {
                        if (connectionPool != null) {
                            connectionPool.returnObject(conn);
                        } else {
                            conn.close();
                        }
                    }
                });

                return future;

            } catch (Throwable t) {
                if (connectionPool != null) {
                    connectionPool.returnObject(conn);
                } else {
                    conn.close();
                }
                throw t;
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the client resources for advanced configuration.
     *
     * @return client resources
     */
    public @NotNull ClientResources getClientResources() {
        return clientResources;
    }

    /**
     * Gets the configuration.
     *
     * @return Redis configuration
     */
    public @NotNull RedisConfiguration getConfiguration() {
        return configuration;
    }
}
