package net.magnesiumbackend.shell.test.ir;

import net.magnesiumbackend.shell.ir.ArgumentSchema;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ArgumentSchemaTest {

    @Test
    void emptySchemaHasNoArguments() {
        ArgumentSchema schema = ArgumentSchema.empty();
        assertTrue(schema.all().isEmpty());
    }

    @Test
    void builderAddsArgument() {
        ArgumentSchema schema = ArgumentSchema.builder()
            .arg("name", String.class)
            .add()
            .build();

        assertTrue(schema.has("name"));
        assertEquals(String.class, schema.get("name").type());
    }

    @Test
    void builderAddsWithDefault() {
        ArgumentSchema schema = ArgumentSchema.builder()
            .arg("port", Integer.class)
            .defaultValue(8080)
            .add()
            .build();

        assertEquals(Integer.class, schema.get("port").type());
        assertEquals(8080, schema.get("port").defaultValue());
    }

    @Test
    void builderAddsRequired() {
        ArgumentSchema schema = ArgumentSchema.builder()
            .arg("env", String.class)
            .required()
            .add()
            .build();

        assertTrue(schema.get("env").required());
    }

    @Test
    void optionalArgumentNotRequired() {
        ArgumentSchema schema = ArgumentSchema.builder()
            .arg("optional", String.class)
            .add()
            .build();

        assertFalse(schema.get("optional").required());
    }

    @Test
    void getMissingArgumentReturnsNull() {
        ArgumentSchema schema = ArgumentSchema.empty();
        assertNull(schema.get("missing"));
    }

    @Test
    void hasReturnsFalseForMissing() {
        ArgumentSchema schema = ArgumentSchema.empty();
        assertFalse(schema.has("missing"));
    }

    @Test
    void allReturnsImmutableMap() {
        ArgumentSchema schema = ArgumentSchema.builder()
            .arg("key", String.class)
            .add()
            .build();

        assertThrows(UnsupportedOperationException.class, () ->
            schema.all().put("new",
                new ArgumentSchema.ArgumentDef(
                    "new",
                    String.class,
                    false,
                    null,
                    "",
                    false,
                    false
                )
            )
        );
    }

    @Test
    void argumentDefProperties() {
        ArgumentSchema.ArgumentDef def =
            new ArgumentSchema.ArgumentDef(
                "test",
                Integer.class,
                true,
                42,
                "desc",
                false,
                false
            );

        assertEquals("test", def.name());
        assertEquals(Integer.class, def.type());
        assertEquals(42, def.defaultValue());
        assertTrue(def.required());
        assertEquals("desc", def.description());
    }

    @Test
    void multipleArguments() {
        ArgumentSchema schema = ArgumentSchema.builder()
            .arg("host", String.class)
            .defaultValue("localhost")
            .add()
            .arg("port", Integer.class)
            .defaultValue(8080)
            .add()
            .arg("token", String.class)
            .required()
            .add()
            .build();

        assertEquals(3, schema.all().size());
        assertEquals("localhost", schema.get("host").defaultValue());
        assertEquals(8080, schema.get("port").defaultValue());
        assertTrue(schema.get("token").required());
    }

    @Test
    void duplicateNameOverwrites() {
        ArgumentSchema schema = ArgumentSchema.builder()
            .arg("dup", String.class)
            .add()
            .arg("dup", Integer.class)
            .add()
            .build();

        // current implementation uses LinkedHashMap.put -> overwrite behavior
        assertEquals(Integer.class, schema.get("dup").type());
    }

    @Test
    void positionalOrderTracked() {
        ArgumentSchema schema = ArgumentSchema.builder()
            .arg("a", String.class).positional().add()
            .arg("b", String.class).add()
            .arg("c", String.class).positional().add()
            .build();

        assertEquals(2, schema.positionalOrder().size());
        assertEquals("a", schema.positionalOrder().get(0));
        assertEquals("c", schema.positionalOrder().get(1));
    }
}