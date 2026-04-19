package net.magnesiumbackend.amqp.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configures a dead letter queue for failed messages.
 *
 * <p>Messages that fail processing (exceed max retries or are rejected)
 * are automatically routed to the configured dead letter exchange/queue.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @QueueListener(queue = "orders", maxRetries = 3)
 * @DeadLetterQueue(queue = "orders.dlq", exchange = "orders.dlx")
 * public void handleOrder(OrderMessage order) {
 *     // Process order - failures go to DLQ after 3 retries
 * }
 * }</pre>
 *
 * @see QueueListener
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface DeadLetterQueue {

    /**
     * The dead letter queue name.
     *
     * @return DLQ name
     */
    String queue();

    /**
     * The dead letter exchange name.
     *
     * @return DLX name
     */
    String exchange();

    /**
     * Routing key for dead letter messages.
     *
     * @return routing key (default empty, uses original routing key)
     */
    String routingKey() default "";

    /**
     * Whether the DLQ is durable.
     *
     * @return true for durable (default true)
     */
    boolean durable() default true;

    /**
     * TTL for messages in the DLQ (in milliseconds).
     *
     * <p>0 means no expiration.</p>
     *
     * @return TTL in ms (default 0)
     */
    long messageTtl() default 0;

    /**
     * Maximum number of messages in the DLQ.
     *
     * <p>0 means no limit.</p>
     *
     * @return max messages (default 0)
     */
    long maxLength() default 0;
}
