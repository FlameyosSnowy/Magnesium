package net.magnesiumbackend.core.event;

import net.magnesiumbackend.core.annotations.Emit;
import net.magnesiumbackend.core.annotations.Subscribe;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all events in the Magnesium event system.
 *
 * <p>Events are immutable occurrences that represent something that happened
 * in the application domain. They are published via the {@link EventBus}
 * and consumed by {@link Subscribe}-annotated listeners.</p>
 *
 * <p>Each event carries metadata:
 * <ul>
 *   <li><b>eventId</b> - Unique UUID for event tracking and idempotency</li>
 *   <li><b>occurredAt</b> - Timestamp when the event was created</li>
 *   <li><b>principal</b> - The actor that caused the event (user, system, etc.)</li>
 * </ul>
 * </p>
 *
 * <p>Example custom event:</p>
 * <pre>{@code
 * public class OrderPlacedEvent extends Event<UUID> {
 *     private final UUID orderId;
 *     private final List<OrderItem> items;
 *     private final BigDecimal total;
 *
 *     public OrderPlacedEvent(Principal<UUID> principal, UUID orderId,
 *                            List<OrderItem> items, BigDecimal total) {
 *         super(principal);
 *         this.orderId = orderId;
 *         this.items = items;
 *         this.total = total;
 *     }
 *
 *     // getters...
 * }
 * }</pre>
 *
 * @param <ID> the type of the principal identifier
 * @see EventBus
 * @see Subscribe
 * @see Emit
 * @see Principal
 */
public abstract class Event<ID> {
    private final String eventId;
    private final Instant occurredAt;
    private final Principal<ID> principal;  // who caused this event

    /**
     * Creates a new event with the specified principal.
     *
     * <p>The eventId is auto-generated as a random UUID, and occurredAt
     * is set to the current instant.</p>
     *
     * @param principal the actor that caused this event
     */
    protected Event(Principal<ID> principal) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = Instant.now();
        this.principal = principal;
    }

    /**
     * Returns the unique identifier for this event.
     *
     * @return the event UUID as a string
     */
    public String eventId() {
        return eventId;
    }

    /**
     * Returns when this event occurred.
     *
     * @return the timestamp of event creation
     */
    public Instant occurredAt() {
        return occurredAt;
    }

    /**
     * Returns the principal (actor) that caused this event.
     *
     * @return the event principal
     */
    public Principal<ID> principal() {
        return principal;
    }
}