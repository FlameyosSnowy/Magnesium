package net.magnesiumbackend.amqp.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a parameter as the message routing key.
 *
 * <p>Used in listener methods to receive the routing key that the
 * message was published with.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * @QueueListener(queue = "orders")
 * public void handleOrder(OrderMessage order, @RoutingKey String key) {
 *     // key contains the routing key (e.g., "order.created")
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface RoutingKey {
}
