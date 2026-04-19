package net.magnesiumbackend.amqp;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Delivery;
import net.magnesiumbackend.amqp.annotations.QueueListener;
import net.magnesiumbackend.core.json.JsonProvider;
import net.magnesiumbackend.core.services.ServiceContext;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for compile-time generated queue listener invokers.
 *
 * <p>Eliminates runtime reflection by generating explicit code to:
 * <ul>
 *   <li>Create listener instances</li>
 *   <li>Resolve and bind method parameters</li>
 *   <li>Invoke the listener method directly</li>
 * </ul>
 *
 * <p>Generated implementations are created by the annotation processor
 * for each {@code @QueueListener} annotated method.</p>
 *
 * @see QueueListenerRegistry#registerListener(QueueListener, net.magnesiumbackend.amqp.annotations.DeadLetterQueue, QueueListenerInvoker)
 */
public interface QueueListenerInvoker {

    /**
     * Creates the listener instance.
     *
     * <p>Called once during wiring to create the target object that
     * will receive messages.</p>
     *
     * @param ctx service context for dependency injection
     * @return the listener instance
     */
    @NotNull Object createInstance(@NotNull ServiceContext ctx);

    /**
     * Invokes the listener method with the given delivery.
     *
     * <p>Generated implementations directly call the target method with
     * compile-time resolved parameter bindings.</p>
     *
     * @param instance the listener instance
     * @param delivery the RabbitMQ message delivery
     * @param channel the channel for ack/nack operations
     * @param jsonProvider JSON provider for message deserialization
     * @throws Exception if invocation fails
     */
    void invoke(
        @NotNull Object instance,
        @NotNull Delivery delivery,
        @NotNull Channel channel,
        @NotNull JsonProvider jsonProvider
    ) throws Exception;

    /**
     * Returns the queue name this listener handles.
     *
     * @return queue name from {@code @QueueListener} annotation
     */
    @NotNull String getQueue();

    /**
     * Returns the target class name (for logging/debugging).
     *
     * @return fully qualified class name
     */
    @NotNull String getTargetClassName();

    /**
     * Returns the method name (for logging/debugging).
     *
     * @return method name
     */
    @NotNull String getMethodName();
}
