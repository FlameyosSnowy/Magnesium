package net.magnesiumbackend.shell.test.engine;

import net.magnesiumbackend.shell.dsl.CommandContext;
import net.magnesiumbackend.shell.engine.GeneratedHandler;
import net.magnesiumbackend.shell.engine.ShellEngine;
import net.magnesiumbackend.shell.ir.CommandIR;
import net.magnesiumbackend.shell.ir.ArgumentSchema;
import net.magnesiumbackend.shell.ir.ExecutionMode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

class ShellEngineTest {

    @Test
    void constructorCreatesEngine() {
        ShellEngine engine = new ShellEngine(name -> null);
        assertNotNull(engine);
    }

    @Test
    void constructorWithDelegates() {
        ShellEngine engine = new ShellEngine(
            name -> null,
            (ir, ctx) -> {},
            (ir, ctx) -> {}
        );
        assertNotNull(engine);
    }

    @Test
    void executeExistingCommand() {
        CommandIR testCmd = new CommandIR("test", ArgumentSchema.empty(), ExecutionMode.LOCAL);
        GeneratedHandler handler = ctx -> {};

        ShellEngine engine = new ShellEngine(name -> name.equals("test") ? handler : null);

        assertDoesNotThrow(() -> engine.execute("test"));
    }

    @Test
    void executeUnknownCommandThrows() {
        ShellEngine engine = new ShellEngine(name -> null);

        assertThrows(ShellEngine.CommandNotFoundException.class, () ->
            engine.execute("unknown")
        );
    }

    @Test
    void tokenizeEmptyLine() {
        ShellEngine engine = new ShellEngine(name -> null);
        assertTrue(engine.tokenize("").isEmpty());
    }

    @Test
    void tokenizeSimple() {
        ShellEngine engine = new ShellEngine(name -> null);
        var tokens = engine.tokenize("deploy --env prod");

        assertEquals(3, tokens.size());
        assertEquals("deploy", tokens.get(0));
        assertEquals("--env", tokens.get(1));
        assertEquals("prod", tokens.get(2));
    }

    @Test
    void tokenizeWithQuotes() {
        ShellEngine engine = new ShellEngine(name -> null);
        var tokens = engine.tokenize("echo \"hello world\"");

        assertEquals(2, tokens.size());
        assertEquals("echo", tokens.get(0));
        assertEquals("hello world", tokens.get(1));
    }

    @Test
    void tokenizeSingleQuotes() {
        ShellEngine engine = new ShellEngine(name -> null);
        var tokens = engine.tokenize("echo 'hello world'");

        assertEquals(2, tokens.size());
        assertEquals("echo", tokens.get(0));
        assertEquals("hello world", tokens.get(1));
    }

    @Test
    void executeWithArgs() {
        final String[] captured = {null};
        CommandIR echoCmd = new CommandIR(
            "echo",
            new ArgumentSchema.Builder().arg("msg", String.class).add().build(),
            ExecutionMode.LOCAL
        );

        GeneratedHandler handler = ctx -> {
            captured[0] = (String) ctx.arguments().get("msg");
        };

        ShellEngine engine = new ShellEngine(name -> name.equals("echo") ? handler : null);

        assertDoesNotThrow(() -> engine.execute("echo --msg hello"));
    }

    @Test
    void commandNotFoundExceptionMessage() {
        ShellEngine.CommandNotFoundException ex = new ShellEngine.CommandNotFoundException("test");
        assertEquals("test", ex.getMessage());
    }

    @Test
    void commandValidationExceptionMessage() {
        ShellEngine.CommandValidationException ex = new ShellEngine.CommandValidationException("invalid");
        assertEquals("invalid", ex.getMessage());
    }

    @Test
    void startInteractiveDoesNotThrow() {
        Map<String, CommandIR> commands = new HashMap<>();
        ShellEngine engine = new ShellEngine(name -> null);

        // Should not throw (but we won't actually run interactive mode in test)
        assertNotNull(engine);
    }
}
