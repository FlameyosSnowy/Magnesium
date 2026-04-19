package net.magnesiumbackend.amqp.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a listener for messages from a RabbitMQ queue.
 *
 * <p>The annotated method will be invoked whenever a message arrives on the
 * specified queue. Methods can accept the message payload, headers, and other
 * metadata as parameters.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @QueueListener(queue = "orders", concurrency = 5)
 * public void handleOrder(OrderMessage order, MessageProperties properties) {
 *     // Process order
 * }
 * }</pre>
 *
 * <p>The method can have various parameter types:</p>
 * <ul>
 *   <li>Message payload (deserialized from JSON)</li>
 *   <li>{@link com.rabbitmq.client.Delivery} - raw delivery</li>
 *   <li>{@link com.rabbitmq.client.Envelope} - message envelope</li>
 *   <li>{@link com.rabbitmq.client.AMQP.BasicProperties} - message properties</li>
 *   <li>{@link String} - routing key</li>
 *   <li>{@link String} channel name (annotated with @Channel)</li>
 * </ul>
 *
 * @see Exchange
 * @see RoutingKey
 * @see DeadLetterQueue
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface QueueListener {

    /**
     * The queue name to listen on.
     *
     * @return the queue name
     */
    String queue();

    /**
     * Number of concurrent consumers for this queue.
     *
     * @return number of consumers (default 1)
     */
    int concurrency() default 1;

    /**
     * Whether to auto-acknowledge messages.
     *
     * <p>If false (default), the listener must manually acknowledge messages.
     * Failed processing can be signaled by throwing an exception.</p>
     *
     * @return true for auto-ack
     */
    boolean autoAck() default false;

    /**
     * Maximum number of retries for failed messages.
     *
     * <p>After max retries, message goes to dead letter queue if configured.</p>
     *
     * @return max retry count (default 3)
     */
    int maxRetries() default 3;

    /**
     * Retry delay in milliseconds between attempts.
     *
     * @return delay in ms (default 1000)
     */
    long retryDelay() default 1000;

    /**
     * Whether the queue is durable (survives broker restart).
     *
     * @return true for durable queue (default true)
     */
    boolean durable() default true;

    /**
     * Whether the queue is exclusive (only this connection can access).
     *
     * @return true for exclusive queue (default false)
     */
    boolean exclusive() default false;

    /**
     * Whether to auto-delete the queue when no consumers.
     *
     * @return true for auto-delete (default false)
     */
    boolean autoDelete() default false;

    /**
     * Arguments for queue declaration (TTL, max length, etc).
     *
     * @return queue arguments
     */
    QueueArgument[] arguments() default {};
}
