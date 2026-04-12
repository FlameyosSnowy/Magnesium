package net.magnesiumbackend.core.http;

import net.magnesiumbackend.core.MagnesiumApplication;
import net.magnesiumbackend.core.route.HttpRouteRegistry;

/**
 * Interface for HTTP transport implementations that can bind to a port
 * and serve requests using the Magnesium framework.
 */
public interface MagnesiumTransport {
    /**
     * Binds the transport to the specified port and starts serving requests.
     *
     * @param port The port to bind to
     * @param application The Magnesium application configuration
     * @param routes The route registry
     */
    void bind(int port, MagnesiumApplication application, HttpRouteRegistry routes);

    /**
     * Shuts down the transport gracefully.
     */
    void shutdown();

    /**
     * Returns the actual port the transport is bound to.
     *
     * @return The port number, or -1 if not bound
     */
    int getPort();

    /**
     * Returns the execution model supported by this transport.
     *
     * <p>Defaults to {@link TransportExecutionModel#BLOCKING} for transports
     * that don't explicitly declare their model.</p>
     *
     * @return The transport's execution model
     */
    default TransportExecutionModel executionModel() {
        return TransportExecutionModel.BLOCKING;
    }

    /**
     * Returns true if this transport supports non-blocking/async execution.
     *
     * <p>This is a convenience method equivalent to checking if
     * {@link #executionModel()} is {@code NON_BLOCKING} or {@code ADAPTIVE}.</p>
     *
     * @return true if async execution is supported
     */
    default boolean supportsAsyncExecution() {
        TransportExecutionModel model = executionModel();
        return model == TransportExecutionModel.NON_BLOCKING || model == TransportExecutionModel.ADAPTIVE;
    }
}