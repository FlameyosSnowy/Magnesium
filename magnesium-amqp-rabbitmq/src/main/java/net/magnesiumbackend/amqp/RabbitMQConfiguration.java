package net.magnesiumbackend.amqp;

import net.magnesiumbackend.core.annotations.ApplicationProperties;
import net.magnesiumbackend.core.annotations.ApplicationProperty;

import java.time.Duration;

/**
 * Configuration properties for RabbitMQ connections.
 *
 * <p>Configure in application.toml:</p>
 * <pre>
 * [rabbitmq]
 * host = "localhost"
 * port = 5672
 * username = "guest"
 * password = "guest"
 * virtual-host = "/"
 * connection-timeout = "10s"
 * requested-heartbeat = "30s"
 * </pre>
 */
@ApplicationProperties(prefix = "rabbitmq")
public class RabbitMQConfiguration {

    @ApplicationProperty(name = "host", defaultValue = "localhost")
    private String host;

    @ApplicationProperty(name = "port", defaultValue = "5672")
    private int port;

    @ApplicationProperty(name = "username", defaultValue = "guest")
    private String username;

    @ApplicationProperty(name = "password", defaultValue = "guest")
    private String password;

    @ApplicationProperty(name = "virtual-host", defaultValue = "/")
    private String virtualHost;

    @ApplicationProperty(name = "connection-timeout", defaultValue = "10s")
    private Duration connectionTimeout;

    @ApplicationProperty(name = "requested-heartbeat", defaultValue = "30s")
    private Duration requestedHeartbeat;

    @ApplicationProperty(name = "automatic-recovery", defaultValue = "true")
    private boolean automaticRecovery;

    @ApplicationProperty(name = "network-recovery-interval", defaultValue = "5s")
    private Duration networkRecoveryInterval;

    @ApplicationProperty(name = "topology-recovery", defaultValue = "true")
    private boolean topologyRecovery;

    // Cluster configuration
    @ApplicationProperty(name = "cluster.enabled", defaultValue = "false")
    private boolean clusterEnabled;

    @ApplicationProperty(name = "cluster.addresses")
    private String clusterAddresses;

    // Connection pooling
    @ApplicationProperty(name = "pool.min-size", defaultValue = "2")
    private int poolMinSize;

    @ApplicationProperty(name = "pool.max-size", defaultValue = "10")
    private int poolMaxSize;

    // SSL
    @ApplicationProperty(name = "ssl.enabled", defaultValue = "false")
    private boolean sslEnabled;

    @ApplicationProperty(name = "ssl.protocol", defaultValue = "TLSv1.2")
    private String sslProtocol;

    // Getters
    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getVirtualHost() { return virtualHost; }
    public Duration getConnectionTimeout() { return connectionTimeout; }
    public Duration getRequestedHeartbeat() { return requestedHeartbeat; }
    public boolean isAutomaticRecovery() { return automaticRecovery; }
    public Duration getNetworkRecoveryInterval() { return networkRecoveryInterval; }
    public boolean isTopologyRecovery() { return topologyRecovery; }
    public boolean isClusterEnabled() { return clusterEnabled; }
    public String getClusterAddresses() { return clusterAddresses; }
    public int getPoolMinSize() { return poolMinSize; }
    public int getPoolMaxSize() { return poolMaxSize; }
    public boolean isSslEnabled() { return sslEnabled; }
    public String getSslProtocol() { return sslProtocol; }
}
