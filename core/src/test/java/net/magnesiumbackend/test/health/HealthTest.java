package net.magnesiumbackend.test.health;

import net.magnesiumbackend.core.health.Health;
import net.magnesiumbackend.core.health.HealthStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Health API.
 */
class HealthTest {

    @Test
    void healthUpCreatesUpStatus() {
        Health health = Health.up().build();
        assertEquals(HealthStatus.UP, health.status());
        assertTrue(health.isUp());
        assertFalse(health.isDown());
    }

    @Test
    void healthDownCreatesDownStatus() {
        Health health = Health.down().build();
        assertEquals(HealthStatus.DOWN, health.status());
        assertTrue(health.isDown());
        assertFalse(health.isUp());
    }

    @Test
    void healthUnknownCreatesUnknownStatus() {
        Health health = Health.unknown().build();
        assertEquals(HealthStatus.UNKNOWN, health.status());
    }

    @Test
    void healthOutOfServiceCreatesOutOfServiceStatus() {
        Health health = Health.outOfService().build();
        assertEquals(HealthStatus.OUT_OF_SERVICE, health.status());
    }

    @Test
    void healthWithDetails() {
        Health health = Health.up()
            .withDetail("version", "1.0.0")
            .withDetail("connections", 42)
            .build();

        assertEquals("1.0.0", health.details().get("version"));
        assertEquals(42, health.details().get("connections"));
    }

    @Test
    void healthWithException() {
        RuntimeException ex = new RuntimeException("Connection failed");
        Health health = Health.down()
            .withException(ex)
            .build();

        assertEquals("Connection failed", health.details().get("error"));
        assertEquals("RuntimeException", health.details().get("exception"));
    }

    @Test
    void healthWithNullDetail() {
        Health health = Health.up()
            .withDetail("value", null)
            .build();

        assertNull(health.details().get("value"));
    }

    @Test
    void healthEquality() {
        Health health1 = Health.up().withDetail("key", "value").build();
        Health health2 = Health.up().withDetail("key", "value").build();
        Health health3 = Health.down().withDetail("key", "value").build();

        assertEquals(health1, health2);
        assertNotEquals(health1, health3);
        assertEquals(health1.hashCode(), health2.hashCode());
    }

    @Test
    void healthToString() {
        Health health = Health.up().build();
        String str = health.toString();
        assertTrue(str.contains("UP"));
        assertTrue(str.contains("Health"));
    }

    @Test
    void emptyHealthDetailsAreUnmodifiable() {
        Health health = Health.up().build();
        assertThrows(UnsupportedOperationException.class, () ->
            health.details().put("new", "value")
        );
    }

    @Test
    void nonEmptyHealthDetailsAreUnmodifiable() {
        Health health = Health.up().withDetail("existing", "value").build();
        assertThrows(UnsupportedOperationException.class, () ->
            health.details().put("new", "value")
        );
    }
}
