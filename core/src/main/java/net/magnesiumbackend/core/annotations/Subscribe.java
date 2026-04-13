package net.magnesiumbackend.core.annotations;

import net.magnesiumbackend.core.annotations.enums.EventPriority;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an event subscriber, invoked when matching events are published.
 *
 * <p>Annotated methods are registered at compile time with the
 * {@link net.magnesiumbackend.core.event.EventBus}. When an event of the matching
 * type is published, the method is called with the event as its parameter.</p>
 *
 * <p>The method signature must accept exactly one parameter: the event type to listen for.
 * Return values are ignored.</p>
 *
 * <p>Subscribers can be ordered by {@link EventPriority} and can choose to ignore
 * cancelled events.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @RestController
 * public class OrderEventListeners {
 *     @Subscribe(priority = EventPriority.HIGH, ignoresCancelled = false)
 *     public void onOrderPlaced(OrderPlacedEvent event) {
 *         // Send confirmation email immediately
 *         emailService.sendOrderConfirmation(event.orderId());
 *     }
 *
 *     @Subscribe(priority = EventPriority.NORMAL, ignoresCancelled = true)
 *     public void onOrderPlacedUpdateInventory(OrderPlacedEvent event) {
 *         // Update inventory - only if event wasn't cancelled by higher priority
 *         inventoryService.reserve(event.items());
 *     }
 *
 *     @Subscribe(priority = EventPriority.MONITOR, ignoresCancelled = false)
 *     public void onOrderPlacedAudit(OrderPlacedEvent event) {
 *         // Audit logging - runs regardless of cancellation state
 *         auditService.log("ORDER_PLACED", event);
 *     }
 * }
 * }</pre>
 *
 * @see Emit
 * @see EventPriority
 * @see net.magnesiumbackend.core.event.Event
 * @see net.magnesiumbackend.core.event.EventBus
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface Subscribe {
    /**
     * The priority for this subscriber relative to others for the same event type.
     *
     * <p>Higher priority subscribers run first. {@link EventPriority#MONITOR} runs
     * last and cannot cancel events.</p>
     *
     * @return the subscriber priority
     */
    EventPriority priority();

    /**
     * Whether this subscriber should be skipped if the event has been cancelled.
     *
     * <p>Set to {@code true} to skip processing cancelled events, or {@code false}
     * to process them anyway (e.g., for logging/auditing).</p>
     *
     * @return true if cancelled events should be ignored
     */
    boolean ignoresCancelled();
}
