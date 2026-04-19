package net.magnesiumbackend.redis;

import net.magnesiumbackend.core.annotations.ApplicationProperties;
import net.magnesiumbackend.core.annotations.ApplicationProperty;

import java.time.Duration;

/**
 * Configuration properties for Redis connections.
 *
 * <p>Configure in application.toml:</p>
 * <pre>
 * [redis]
 * host = "localhost"
 * port = 6379
 * password = "secret"
 * database = 0
 * timeout = "5s"
 * pool.max-active = 8
 * pool.max-idle = 8
 *
 * # For cluster mode
 * cluster.nodes = ["localhost:7000", "localhost:7001", "localhost:7002"]
 *
 * # For sentinel mode
 * sentinel.master = "mymaster"
 * sentinel.nodes = ["localhost:26379", "localhost:26380"]
 * </pre>
 */
@ApplicationProperties(prefix = "redis")
public class RedisConfiguration {

    @ApplicationProperty(name = "host", defaultValue = "localhost")
    private String host;

    @ApplicationProperty(name = "port", defaultValue = "6379")
    private int port;

    @ApplicationProperty(name = "password", defaultValue = "")
    private String password;

    @ApplicationProperty(name = "database", defaultValue = "0")
    private int database;

    @ApplicationProperty(name = "timeout", defaultValue = "5s")
    private Duration timeout;

    @ApplicationProperty(name = "client-name", defaultValue = "magnesium")
    private String clientName;

    // Connection pool settings
    @ApplicationProperty(name = "pool.max-active", defaultValue = "8")
    private int poolMaxActive;

    @ApplicationProperty(name = "pool.max-idle", defaultValue = "8")
    private int poolMaxIdle;

    @ApplicationProperty(name = "pool.min-idle", defaultValue = "0")
    private int poolMinIdle;

    @ApplicationProperty(name = "pool.max-wait", defaultValue = "-1ms")
    private Duration poolMaxWait;

    // Cluster configuration
    @ApplicationProperty(name = "cluster.enabled", defaultValue = "false")
    private boolean clusterEnabled;

    @ApplicationProperty(name = "cluster.nodes")
    private String clusterNodes;

    @ApplicationProperty(name = "cluster.max-redirects", defaultValue = "3")
    private int clusterMaxRedirects;

    // Sentinel configuration
    @ApplicationProperty(name = "sentinel.enabled", defaultValue = "false")
    private boolean sentinelEnabled;

    @ApplicationProperty(name = "sentinel.master", defaultValue = "")
    private String sentinelMaster;

    @ApplicationProperty(name = "sentinel.nodes")
    private String sentinelNodes;

    // SSL/TLS
    @ApplicationProperty(name = "ssl.enabled", defaultValue = "false")
    private boolean sslEnabled;

    @ApplicationProperty(name = "ssl.verify-mode", defaultValue = "full")
    private String sslVerifyMode;

    // Pub/Sub
    @ApplicationProperty(name = "pubsub.enabled", defaultValue = "true")
    private boolean pubSubEnabled;

    @ApplicationProperty(name = "pubsub.thread-pool-size", defaultValue = "4")
    private int pubSubThreadPoolSize;

    // Getters
    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getPassword() { return password; }
    public int getDatabase() { return database; }
    public Duration getTimeout() { return timeout; }
    public String getClientName() { return clientName; }
    public int getPoolMaxActive() { return poolMaxActive; }
    public int getPoolMaxIdle() { return poolMaxIdle; }
    public int getPoolMinIdle() { return poolMinIdle; }
    public Duration getPoolMaxWait() { return poolMaxWait; }
    public boolean isClusterEnabled() { return clusterEnabled; }
    public String getClusterNodes() { return clusterNodes; }
    public int getClusterMaxRedirects() { return clusterMaxRedirects; }
    public boolean isSentinelEnabled() { return sentinelEnabled; }
    public String getSentinelMaster() { return sentinelMaster; }
    public String getSentinelNodes() { return sentinelNodes; }
    public boolean isSslEnabled() { return sslEnabled; }
    public String getSslVerifyMode() { return sslVerifyMode; }
    public boolean isPubSubEnabled() { return pubSubEnabled; }
    public int getPubSubThreadPoolSize() { return pubSubThreadPoolSize; }
}
