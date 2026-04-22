package net.magnesiumbackend.test.health;

import net.magnesiumbackend.core.health.HealthStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HealthStatus enum.
 */
class HealthStatusTest {

    @Test
    void healthStatusCodes() {
        assertEquals("UP", HealthStatus.UP.code());
        assertEquals("DOWN", HealthStatus.DOWN.code());
        assertEquals("UNKNOWN", HealthStatus.UNKNOWN.code());
        assertEquals("OUT_OF_SERVICE", HealthStatus.OUT_OF_SERVICE.code());
    }

    @Test
    void healthStatusSeverities() {
        assertEquals(0, HealthStatus.UP.severity());
        assertEquals(1, HealthStatus.UNKNOWN.severity());
        assertEquals(2, HealthStatus.OUT_OF_SERVICE.severity());
        assertEquals(3, HealthStatus.DOWN.severity());
    }

    @ParameterizedTest
    @CsvSource({
        "UP, UP, UP",
        "UP, DOWN, DOWN",
        "DOWN, UP, DOWN",
        "UP, UNKNOWN, UNKNOWN",
        "UNKNOWN, UP, UNKNOWN",
        "DOWN, OUT_OF_SERVICE, DOWN",
        "OUT_OF_SERVICE, DOWN, DOWN"
    })
    void healthStatusCombine(String status1, String status2, String expected) {
        HealthStatus s1 = HealthStatus.valueOf(status1);
        HealthStatus s2 = HealthStatus.valueOf(status2);
        HealthStatus exp = HealthStatus.valueOf(expected);

        assertEquals(exp, s1.combine(s2));
        assertEquals(exp, s2.combine(s1)); // commutative
    }

    @Test
    void healthStatusToStringReturnsCode() {
        assertEquals("UP", HealthStatus.UP.toString());
        assertEquals("DOWN", HealthStatus.DOWN.toString());
    }

    @Test
    void severityOrdering() {
        assertTrue(HealthStatus.UP.severity() < HealthStatus.UNKNOWN.severity());
        assertTrue(HealthStatus.UNKNOWN.severity() < HealthStatus.OUT_OF_SERVICE.severity());
        assertTrue(HealthStatus.OUT_OF_SERVICE.severity() < HealthStatus.DOWN.severity());
    }
}
