package net.magnesiumbackend.test.config;

import net.magnesiumbackend.core.config.ConfigSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;

class MapConfigSourceTest {

    @Test
    void nameReturnsProvidedName() {
        ConfigSource source = ConfigSource.ofMap("test-map", Map.of());
        assertEquals("test-map", source.name());
    }

    @Test
    void hasExistingKey() {
        ConfigSource source = ConfigSource.ofMap("test", Map.of("key", "value"));
        assertTrue(source.has("key"));
    }

    @Test
    void hasNonExistingKey() {
        ConfigSource source = ConfigSource.ofMap("test", Map.of());
        assertFalse(source.has("missing"));
    }

    @Test
    void getStringReturnsStringValue() {
        ConfigSource source = ConfigSource.ofMap("test", Map.of("key", "value"));
        assertEquals("value", source.getString("key"));
    }

    @Test
    void getStringConvertsNonString() {
        ConfigSource source = ConfigSource.ofMap("test", Map.of("key", 123));
        assertEquals("123", source.getString("key"));
    }

    @Test
    void getStringReturnsNullForMissing() {
        ConfigSource source = ConfigSource.ofMap("test", Map.of());
        assertNull(source.getString("missing"));
    }

    @Test
    void getIntFromNumber() {
        ConfigSource source = ConfigSource.ofMap("test", Map.of("key", 42));
        assertEquals(Integer.valueOf(42), source.getInt("key"));
    }

    @Test
    void getIntFromString() {
        ConfigSource source = ConfigSource.ofMap("test", Map.of("key", "123"));
        assertEquals(Integer.valueOf(123), source.getInt("key"));
    }

    @Test
    void getIntReturnsNullForMissing() {
        ConfigSource source = ConfigSource.ofMap("test", Map.of());
        assertNull(source.getInt("missing"));
    }

    @Test
    void getLongFromNumber() {
        ConfigSource source = ConfigSource.ofMap("test", Map.of("key", 9999999999L));
        assertEquals(Long.valueOf(9999999999L), source.getLong("key"));
    }

    @Test
    void getBooleanFromBoolean() {
        ConfigSource source = ConfigSource.ofMap("test", Map.of("key", true));
        assertEquals(Boolean.TRUE, source.getBoolean("key"));
    }

    @Test
    void getBooleanFromString() {
        ConfigSource source = ConfigSource.ofMap("test", Map.of("key", "true"));
        assertEquals(Boolean.TRUE, source.getBoolean("key"));
    }

    @Test
    void nestedPathAccess() {
        ConfigSource source = ConfigSource.ofMap("test", Map.of(
            "database", Map.of(
                "host", "localhost",
                "port", 5432
            )
        ));

        assertEquals("localhost", source.getString("database.host"));
        assertEquals(Integer.valueOf(5432), source.getInt("database.port"));
    }

    @Test
    void deeplyNestedPath() {
        ConfigSource source = ConfigSource.ofMap("test", Map.of(
            "a", Map.of(
                "b", Map.of(
                    "c", "deep-value"
                )
            )
        ));

        assertEquals("deep-value", source.getString("a.b.c"));
    }

    @Test
    void nestedPathMissing() {
        ConfigSource source = ConfigSource.ofMap("test", Map.of("a", "value"));
        assertNull(source.getString("a.b"));
    }

    @Test
    void extractSingleElementList() {
        ConfigSource source = ConfigSource.ofMap("test", Map.of(
            "key", List.of("single-value")
        ));
        assertEquals("single-value", source.getString("key"));
    }

    @Test
    void emptyKeyReturnsNull() {
        ConfigSource source = ConfigSource.ofMap("test", Map.of("key", "value"));
        assertNull(source.getString(""));
    }

    @Test
    void nonMapPathSegmentReturnsNull() {
        ConfigSource source = ConfigSource.ofMap("test", Map.of(
            "string-value", "not-a-map"
        ));
        assertNull(source.getString("string-value.nested"));
    }
}
