package net.magnesiumbackend.amqp.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a binding between a queue and an exchange.
 *
 * <p>Used with {@link QueueListener} to set up queue-to-exchange bindings
 * with routing keys.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @QueueListener(queue = "orders.created")
 * @Exchange(name = "orders", type = ExchangeType.TOPIC)
 * @Binding(exchange = "orders", routingKey = "order.created")
 * @Binding(exchange = "orders", routingKey = "order.updated")
 * public void handleOrder(OrderMessage order) {
 *     // Handle both created and updated orders
 * }
 * }</pre>
 *
 * @see QueueListener
 * @see Exchange
 */
@Repeatable(Bindings.class)
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface Binding {

    /**
     * The exchange name to bind to.
     *
     * @return exchange name
     */
    String exchange();

    /**
     * The routing key pattern for this binding.
     *
     * <p>For topic exchanges, use patterns like "order.*" or "order.#".</p>
     *
     * @return routing key
     */
    String routingKey() default "";

    /**
     * Arguments for the binding.
     *
     * @return binding arguments
     */
    QueueArgument[] arguments() default {};
}

