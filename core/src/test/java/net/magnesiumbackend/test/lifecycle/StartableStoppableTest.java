package net.magnesiumbackend.test.lifecycle;

import net.magnesiumbackend.core.lifecycle.Startable;
import net.magnesiumbackend.core.lifecycle.Stoppable;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Startable and Stoppable interfaces.
 */
class StartableStoppableTest {

    @Test
    void startableCanBeStarted() {
        Startable startable = () -> {};
        assertDoesNotThrow(startable::start);
    }

    @Test
    void stoppableCanBeStopped() {
        Stoppable stoppable = () -> {};
        assertDoesNotThrow(stoppable::stop);
    }

    @Test
    void startableThrowsOnError() {
        Startable failing = () -> { throw new RuntimeException("Start failed"); };
        assertThrows(RuntimeException.class, failing::start);
    }

    @Test
    void stoppableThrowsOnError() {
        Stoppable failing = () -> { throw new RuntimeException("Stop failed"); };
        assertThrows(RuntimeException.class, failing::stop);
    }

    @Test
    void classImplementsBoth() {
        class DualLifecycle implements Startable, Stoppable {
            boolean started = false;
            boolean stopped = false;

            @Override
            public void start() { started = true; }

            @Override
            public void stop() { stopped = true; }
        }

        DualLifecycle service = new DualLifecycle();
        service.start();
        service.stop();

        assertTrue(service.started);
        assertTrue(service.stopped);
    }

    @Test
    void functionalInterfaceCompatibility() {
        // Test that Startable can be used as lambda
        Startable s1 = () -> System.out.println("starting");

        // Test that Stoppable can be used as lambda
        Stoppable s2 = () -> System.out.println("stopping");

        assertDoesNotThrow(() -> {
            s1.start();
            s2.stop();
        });
    }
}
