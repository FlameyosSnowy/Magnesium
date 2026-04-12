package net.magnesiumbackend.core.http;

/**
 * Defines the execution model supported by a transport implementation.
 *
 * <p>Transports can be categorized by their threading model:</p>
 * <ul>
 *   <li><b>BLOCKING</b>: Synchronous I/O where each request blocks a thread
 *       (e.g., Tomcat with traditional servlet model)</li>
 *   <li><b>NON_BLOCKING</b>: Event-driven I/O that doesn't block threads during I/O
 *       (e.g., Netty, Undertow with async servlet)</li>
 *   <li><b>ADAPTIVE</b>: Can operate in either mode depending on configuration
 *       (e.g., Tomcat with NIO connector, configurable thread pools)</li>
 * </ul>
 *
 * @see MagnesiumTransport
 */
public enum TransportExecutionModel {
    /**
     * Synchronous/blocking execution model.
     * Each request occupies a thread for its entire duration.
     */
    BLOCKING,

    /**
     * Asynchronous/non-blocking execution model.
     * Threads are not blocked during I/O operations.
     */
    NON_BLOCKING,

    /**
     * Adaptive execution model that can switch between blocking and non-blocking
     * based on configuration or runtime conditions.
     */
    ADAPTIVE
}
