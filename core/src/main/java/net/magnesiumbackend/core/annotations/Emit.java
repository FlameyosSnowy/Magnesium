package net.magnesiumbackend.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method of a service as an event emitter proxy.
 *
 * <p>Annotated methods are intercepted at runtime by a generated proxy that
 * publishes an event to the {@link net.magnesiumbackend.core.event.EventBus}
 * after the method completes successfully.</p>
 *
 * <p>This enables event-driven architecture without manual event publishing.
 * The service method focuses on business logic; the framework handles event
 * distribution to subscribers.</p>
 *
 * <p>The method must return an {@link net.magnesiumbackend.core.event.Event}
 * or a type convertible to an event. The returned event is automatically published.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * public class OrderService {
 *     @Emit
 *     public OrderPlacedEvent createOrder(CreateOrderRequest request) {
 *         Order order = orderRepository.save(request.toOrder());
 *         // Method return value is automatically published as an event
 *         return new OrderPlacedEvent(order.id(), order.items(), order.total());
 *     }
 * }
 * }</pre>
 *
 * <p>Subscribers receive the event:</p>
 * <pre>{@code
 * public class OrderListeners {
 *     @Subscribe(priority = EventPriority.NORMAL, ignoresCancelled = false)
 *     public void onOrderPlaced(OrderPlacedEvent event) {
 *         emailService.sendConfirmation(event);
 *     }
 * }
 * }</pre>
 *
 * @see Subscribe
 * @see net.magnesiumbackend.core.event.Event
 * @see net.magnesiumbackend.core.event.EventBus
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface Emit {
}
