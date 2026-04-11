package net.magnesiumbackend.core.event;

import java.time.Instant;
import java.util.UUID;

public abstract class Event<ID> {
    private final String eventId;
    private final Instant occurredAt;
    private final Principal<ID> principal;  // who caused this event

    protected Event(Principal<ID> principal) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = Instant.now();
        this.principal = principal;
    }

    public String eventId() {
        return eventId;
    }

    public Instant occurredAt() {
        return occurredAt;
    }

    public Principal<ID> principal() {
        return principal;
    }
}