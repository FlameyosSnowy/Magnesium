package net.magnesiumbackend.shell.test.dsl;

import net.magnesiumbackend.shell.dsl.CommandContext;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

class CommandContextTest {

    @Test
    void constructorSetsCommandName() {
        Map<String, Object> args = new HashMap<>();
        CommandContext ctx = new CommandContext("test", args);
        assertEquals("test", ctx.commandName());
    }

    @Test
    void constructorSetsArguments() {
        Map<String, Object> args = new HashMap<>();
        args.put("key", "value");
        CommandContext ctx = new CommandContext("test", args);

        assertEquals("value", ctx.arguments().get("key"));
    }

    @Test
    void getReturnsValue() {
        Map<String, Object> args = new HashMap<>();
        args.put("key", "value");
        CommandContext ctx = new CommandContext("test", args);

        assertEquals("value", ctx.get("key"));
    }

    @Test
    void getMissingReturnsNull() {
        CommandContext ctx = new CommandContext("test", Map.of());
        assertNull(ctx.get("missing"));
    }

    @Test
    void hasExistingKey() {
        Map<String, Object> args = new HashMap<>();
        args.put("key", "value");
        CommandContext ctx = new CommandContext("test", args);

        assertTrue(ctx.has("key"));
    }

    @Test
    void hasMissingKey() {
        CommandContext ctx = new CommandContext("test", Map.of());
        assertFalse(ctx.has("missing"));
    }

    @Test
    void typedGet() {
        Map<String, Object> args = new HashMap<>();
        args.put("num", 42);
        CommandContext ctx = new CommandContext("test", args);

        Integer value = ctx.get("num", Integer.class);
        assertEquals(42, value);
    }

    @Test
    void typedGetWrongType() {
        Map<String, Object> args = new HashMap<>();
        args.put("str", "hello");
        CommandContext ctx = new CommandContext("test", args);

        assertThrows(ClassCastException.class, () ->
            ctx.get("str", Integer.class)
        );
    }

    @Test
    void typedGetMissing() {
        CommandContext ctx = new CommandContext("test", Map.of());
        assertNull(ctx.get("missing", String.class));
    }

    @Test
    void getOrDefaultExisting() {
        Map<String, Object> args = new HashMap<>();
        args.put("key", "value");
        CommandContext ctx = new CommandContext("test", args);

        assertEquals("value", ctx.getOrDefault("key", "default"));
    }

    @Test
    void getOrDefaultMissing() {
        CommandContext ctx = new CommandContext("test", Map.of());
        assertEquals("default", ctx.getOrDefault("missing", "default"));
    }

    @Test
    void emptyContextHasNoArguments() {
        CommandContext ctx = new CommandContext("test", Map.of());
        assertTrue(ctx.arguments().isEmpty());
    }

    @Test
    void constructorCopiesArguments() {
        Map<String, Object> args = new HashMap<>();
        args.put("key", "value");
        CommandContext ctx = new CommandContext("test", args);

        // Modify original map
        args.put("new", "added");

        // Context should not see the change
        assertFalse(ctx.has("new"));
    }

    @Test
    void sizeReturnsArgumentCount() {
        Map<String, Object> args = new HashMap<>();
        args.put("a", 1);
        args.put("b", 2);
        CommandContext ctx = new CommandContext("test", args);

        assertEquals(2, ctx.size());
    }

    @Test
    void keySetReturnsKeys() {
        Map<String, Object> args = new HashMap<>();
        args.put("a", 1);
        args.put("b", 2);
        CommandContext ctx = new CommandContext("test", args);

        assertTrue(ctx.keySet().contains("a"));
        assertTrue(ctx.keySet().contains("b"));
        assertEquals(2, ctx.keySet().size());
    }

    @Test
    void isEmptyTrueWhenNoArgs() {
        CommandContext ctx = new CommandContext("test", Map.of());
        assertTrue(ctx.isEmpty());
    }

    @Test
    void isEmptyFalseWhenHasArgs() {
        Map<String, Object> args = new HashMap<>();
        args.put("key", "value");
        CommandContext ctx = new CommandContext("test", args);

        assertFalse(ctx.isEmpty());
    }
}
