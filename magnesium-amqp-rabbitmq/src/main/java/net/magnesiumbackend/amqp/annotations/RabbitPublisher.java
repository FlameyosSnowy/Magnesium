package net.magnesiumbackend.amqp.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field or parameter for RabbitMQ publisher injection.
 *
 * <p>Provides a typed publisher for sending messages to exchanges.
 * The publisher handles serialization, routing, and error handling.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @Service
 * public class OrderService {
 *
 *     @RabbitPublisher(exchange = "orders", routingKey = "order.created")
 *     private MessagePublisher<OrderEvent> orderPublisher;
 *
 *     public void createOrder(Order order) {
 *         OrderEvent event = new OrderEvent(order.id(), "created");
 *         orderPublisher.publish(event);
 *     }
 * }
 * }</pre>
 *
 * @see Exchange
 * @see RoutingKey
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface RabbitPublisher {

    /**
     * The target exchange name.
     *
     * @return exchange name
     */
    String exchange();

    /**
     * The default routing key.
     *
     * <p>Can be overridden per-message using {@code publish(message, routingKey)}.</p>
     *
     * @return routing key
     */
    String routingKey() default "";

    /**
     * Whether to use mandatory delivery (return if unroutable).
     *
     * @return true for mandatory (default false)
     */
    boolean mandatory() default false;

    /**
     * Message delivery mode.
     *
     * @return 1 for non-persistent, 2 for persistent (default 2)
     */
    int deliveryMode() default 2;

    /**
     * Message priority (0-9).
     *
     * @return priority (default 0)
     */
    int priority() default 0;

    /**
     * Message expiration time in milliseconds.
     *
     * @return TTL in ms (default 0, no expiration)
     */
    long expiration() default 0;
}
