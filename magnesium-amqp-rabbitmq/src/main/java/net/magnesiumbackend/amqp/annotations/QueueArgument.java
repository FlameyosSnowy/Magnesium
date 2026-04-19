package net.magnesiumbackend.amqp.annotations;

/**
 * A single queue or binding argument.
 *
 * <p>RabbitMQ supports various arguments for queue declaration:</p>
 * <ul>
 *   <li>x-message-ttl - message time-to-live</li>
 *   <li>x-expires - queue expiration</li>
 *   <li>x-max-length - max messages in queue</li>
 *   <li>x-max-length-bytes - max queue size in bytes</li>
 *   <li>x-overflow - overflow behavior (drop-head, reject-publish)</li>
 *   <li>x-dead-letter-exchange - DLX for expired/rejected messages</li>
 *   <li>x-dead-letter-routing-key - routing key for DLX</li>
 *   <li>x-max-priority - max priority for priority queue</li>
 *   <li>x-queue-mode - lazy queue mode</li>
 *   <li>x-single-active-consumer - single active consumer</li>
 * </ul>
 *
 * @see QueueListener
 */
public @interface QueueArgument {

    /**
     * The argument name.
     *
     * @return argument name
     */
    String name();

    /**
     * The argument value.
     *
     * @return argument value as string
     */
    String value();
}
