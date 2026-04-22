package net.magnesiumbackend.test.health;

import net.magnesiumbackend.core.health.CompositeHealthIndicator;
import net.magnesiumbackend.core.health.Health;
import net.magnesiumbackend.core.health.HealthIndicator;
import net.magnesiumbackend.core.health.HealthStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Duration;
import java.util.concurrent.Executors;

/**
 * Tests for CompositeHealthIndicator.
 */
class CompositeHealthIndicatorTest {

    @Test
    void emptyCompositeReturnsUp() {
        CompositeHealthIndicator composite = new CompositeHealthIndicator();
        Health health = composite.health();

        assertEquals(HealthStatus.UP, health.status());
    }

    @Test
    void singleUpIndicator() {
        CompositeHealthIndicator composite = new CompositeHealthIndicator();
        composite.add(new TestIndicator("test", Health.up().withDetail("check", true).build()));

        Health health = composite.health();
        assertEquals(HealthStatus.UP, health.status());
        assertTrue(health.details().containsKey("test"));
    }

    @Test
    void singleDownIndicator() {
        CompositeHealthIndicator composite = new CompositeHealthIndicator();
        composite.add(new TestIndicator("test", Health.down().build()));

        Health health = composite.health();
        assertEquals(HealthStatus.DOWN, health.status());
    }

    @Test
    void mixedIndicatorsDownWins() {
        CompositeHealthIndicator composite = new CompositeHealthIndicator();
        composite.add(new TestIndicator("up1", Health.up().build()));
        composite.add(new TestIndicator("down1", Health.down().build()));
        composite.add(new TestIndicator("up2", Health.up().build()));

        Health health = composite.health();
        assertEquals(HealthStatus.DOWN, health.status());
    }

    @Test
    void addWithCustomName() {
        CompositeHealthIndicator composite = new CompositeHealthIndicator();
        composite.add("customName", new TestIndicator("ignored", Health.up().build()));

        Health health = composite.health();
        assertTrue(health.details().containsKey("customName"));
    }

    @Test
    void removeIndicator() {
        CompositeHealthIndicator composite = new CompositeHealthIndicator();
        TestIndicator indicator = new TestIndicator("toRemove", Health.up().build());
        composite.add(indicator);
        composite.remove("toRemove");

        Health health = composite.health();
        assertEquals(HealthStatus.UP, health.status());
        assertFalse(health.details().containsKey("toRemove"));
    }

    @Test
    void defaultNameExtraction() {
        CompositeHealthIndicator composite = new CompositeHealthIndicator();
        composite.add(new DatabaseHealthIndicator());

        Health health = composite.health();
        assertTrue(health.details().containsKey("database"));
    }

    @Test
    void indicatorsReturnsUnmodifiableMap() {
        CompositeHealthIndicator composite = new CompositeHealthIndicator();
        composite.add(new TestIndicator("test", Health.up().build()));

        assertThrows(UnsupportedOperationException.class, () ->
            composite.indicators().put("new", new TestIndicator("new", Health.down().build()))
        );
    }

    @Test
    void customExecutorAndTimeout() {
        CompositeHealthIndicator composite = new CompositeHealthIndicator(
            Executors.newSingleThreadExecutor(),
            Duration.ofSeconds(1)
        );
        composite.add(new TestIndicator("test", Health.up().build()));

        Health health = composite.health();
        assertEquals(HealthStatus.UP, health.status());
    }

    @Test
    void indicatorThrowingExceptionIsDown() {
        CompositeHealthIndicator composite = new CompositeHealthIndicator();
        composite.add(new HealthIndicator() {
            @Override
            public Health health() {
                throw new RuntimeException("Check failed");
            }

            @Override
            public String name() {
                return "failing";
            }
        });

        Health health = composite.health();
        assertEquals(HealthStatus.DOWN, health.status());
    }

    @Test
    void indicatorNameMethod() {
        TestIndicator indicator = new TestIndicator("myIndicator", Health.up().build());
        assertEquals("myIndicator", indicator.name());
    }

    @Test
    void chainingAddReturnsSameInstance() {
        CompositeHealthIndicator composite = new CompositeHealthIndicator();
        CompositeHealthIndicator result = composite.add(new TestIndicator("test", Health.up().build()));
        assertSame(composite, result);
    }

    @Test
    void chainingRemoveReturnsSameInstance() {
        CompositeHealthIndicator composite = new CompositeHealthIndicator();
        composite.add(new TestIndicator("test", Health.up().build()));
        CompositeHealthIndicator result = composite.remove("test");
        assertSame(composite, result);
    }

    // Test helper classes
    static class TestIndicator implements HealthIndicator {
        private final String name;
        private final Health health;

        TestIndicator(String name, Health health) {
            this.name = name;
            this.health = health;
        }

        @Override
        public Health health() {
            return health;
        }

        @Override
        public String name() {
            return name;
        }
    }

    static class DatabaseHealthIndicator implements HealthIndicator {
        @Override
        public Health health() {
            return Health.up().build();
        }

        @Override
        public String name() {
            return "database";
        }
    }
}
