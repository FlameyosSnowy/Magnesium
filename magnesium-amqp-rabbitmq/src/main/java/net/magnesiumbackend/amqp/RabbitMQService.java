package net.magnesiumbackend.amqp;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import net.magnesiumbackend.amqp.annotations.Exchange;
import net.magnesiumbackend.core.lifecycle.Startable;
import net.magnesiumbackend.core.lifecycle.Stoppable;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * Core RabbitMQ service managing connections and channels.
 *
 * <p>Provides connection pooling, channel management, and lifecycle
 * integration with Magnesium's dependency graph.</p>
 */
public class RabbitMQService implements Startable, Stoppable {

    private final RabbitMQConfiguration configuration;
    private final ConnectionFactory connectionFactory;
    private Connection connection;
    private final Map<String, Channel> channels = new ConcurrentHashMap<>();

    public RabbitMQService(@NotNull RabbitMQConfiguration configuration) {
        this.configuration = configuration;
        this.connectionFactory = createConnectionFactory(configuration);
    }

    private ConnectionFactory createConnectionFactory(RabbitMQConfiguration config) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(config.getHost());
        factory.setPort(config.getPort());
        factory.setUsername(config.getUsername());
        factory.setPassword(config.getPassword());
        factory.setVirtualHost(config.getVirtualHost());
        factory.setConnectionTimeout((int) config.getConnectionTimeout().toMillis());
        factory.setRequestedHeartbeat((int) config.getRequestedHeartbeat().getSeconds());
        factory.setAutomaticRecoveryEnabled(config.isAutomaticRecovery());
        factory.setNetworkRecoveryInterval(config.getNetworkRecoveryInterval().toMillis());
        factory.setTopologyRecoveryEnabled(config.isTopologyRecovery());

        if (config.isSslEnabled()) {
            try {
                factory.useSslProtocol(config.getSslProtocol());
            } catch (Exception e) {
                throw new RuntimeException("Failed to configure SSL for RabbitMQ", e);
            }
        }

        return factory;
    }

    @Override
    public void start() throws Exception {
        if (configuration.isClusterEnabled() && configuration.getClusterAddresses() != null) {
            // Cluster connection
            String[] addresses = configuration.getClusterAddresses().split(",");
            com.rabbitmq.client.Address[] addrArray = new com.rabbitmq.client.Address[addresses.length];
            for (int i = 0; i < addresses.length; i++) {
                String[] parts = addresses[i].trim().split(":");
                addrArray[i] = new com.rabbitmq.client.Address(parts[0], Integer.parseInt(parts[1]));
            }
            this.connection = connectionFactory.newConnection(addrArray);
        } else {
            // Single node connection
            this.connection = connectionFactory.newConnection();
        }
    }

    @Override
    public void stop() throws Exception {
        // Close all channels
        for (Channel channel : channels.values()) {
            if (channel.isOpen()) {
                channel.close();
            }
        }
        channels.clear();

        // Close connection
        if (connection != null && connection.isOpen()) {
            connection.close();
        }
    }

    /**
     * Gets or creates a channel.
     *
     * @param name channel identifier
     * @return the channel
     */
    public @NotNull Channel getChannel(@NotNull String name) throws IOException {
        return channels.computeIfAbsent(name, n -> {
            try {
                return connection.createChannel();
            } catch (IOException e) {
                throw new RuntimeException("Failed to create channel: " + n, e);
            }
        });
    }

    /**
     * Declares an exchange.
     *
     * @param exchange the exchange definition
     */
    public void declareExchange(@NotNull Exchange exchange) throws IOException {
        Channel channel = getChannel("admin");
        String type = exchange.type().name().toLowerCase();
        channel.exchangeDeclare(
            exchange.name(),
            type,
            exchange.durable(),
            exchange.autoDelete(),
            exchange.internal(),
            java.util.Collections.emptyMap()
        );
    }

    /**
     * Declares a queue.
     *
     * @param queueName queue name
     * @param durable whether durable
     * @param exclusive whether exclusive
     * @param autoDelete whether auto-delete
     * @param arguments queue arguments
     */
    public void declareQueue(
        @NotNull String queueName,
        boolean durable,
        boolean exclusive,
        boolean autoDelete,
        @NotNull Map<String, Object> arguments
    ) throws IOException {
        Channel channel = getChannel("admin");
        channel.queueDeclare(queueName, durable, exclusive, autoDelete, arguments);
    }

    /**
     * Binds a queue to an exchange.
     *
     * @param queue queue name
     * @param exchange exchange name
     * @param routingKey routing key
     */
    public void bindQueue(@NotNull String queue, @NotNull String exchange, @NotNull String routingKey) throws IOException {
        Channel channel = getChannel("admin");
        channel.queueBind(queue, exchange, routingKey);
    }

    /**
     * Creates a typed message publisher.
     *
     * @param exchange target exchange
     * @param routingKey default routing key
     * @param messageType message class
     * @param jsonProvider JSON serialization provider
     * @return the publisher
     */
    public <T> @NotNull MessagePublisher<T> createPublisher(
        @NotNull String exchange,
        @NotNull String routingKey,
        @NotNull Class<T> messageType,
        @NotNull net.magnesiumbackend.core.json.JsonProvider jsonProvider
    ) {
        return new MessagePublisherImpl<>(this, exchange, routingKey, messageType, jsonProvider);
    }

    /**
     * Checks if the connection is open.
     *
     * @return true if connected
     */
    public boolean isConnected() {
        return connection != null && connection.isOpen();
    }

    /**
     * Gets the underlying connection.
     *
     * @return the connection
     */
    public @NotNull Connection getConnection() {
        if (connection == null || !connection.isOpen()) {
            throw new IllegalStateException("RabbitMQ connection is not open");
        }
        return connection;
    }
}
