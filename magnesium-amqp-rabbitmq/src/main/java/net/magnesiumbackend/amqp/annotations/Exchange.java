package net.magnesiumbackend.amqp.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a RabbitMQ exchange.
 *
 * <p>Can be used on methods to declare an exchange when binding a queue listener,
 * or on classes to declare exchanges at startup.</p>
 *
 * <p>Example usage on a listener:</p>
 * <pre>{@code
 * @QueueListener(queue = "orders")
 * @Exchange(name = "orders.exchange", type = ExchangeType.TOPIC)
 * @Binding(exchange = "orders.exchange", routingKey = "order.created")
 * public void handleOrder(OrderMessage order) {
 *     // Process order
 * }
 * }</pre>
 *
 * @see QueueListener
 * @see Binding
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Exchange {

    /**
     * The exchange name.
     *
     * @return exchange name
     */
    String name();

    /**
     * The exchange type.
     *
     * @return exchange type (default TOPIC)
     */
    ExchangeType type() default ExchangeType.TOPIC;

    /**
     * Whether the exchange is durable.
     *
     * @return true for durable (default true)
     */
    boolean durable() default true;

    /**
     * Whether to auto-delete the exchange when no queues bound.
     *
     * @return true for auto-delete (default false)
     */
    boolean autoDelete() default false;

    /**
     * Whether the exchange is internal (cannot be published to directly).
     *
     * @return true for internal (default false)
     */
    boolean internal() default false;

    /**
     * Alternate exchange for unroutable messages.
     *
     * @return alternate exchange name (default empty)
     */
    String alternateExchange() default "";
}

