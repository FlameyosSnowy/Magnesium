package net.magnesiumbackend.shell.test.input;

import net.magnesiumbackend.shell.engine.ShellEngine;
import net.magnesiumbackend.shell.input.ShellInputSource;
import net.magnesiumbackend.shell.ir.ArgumentSchema;
import net.magnesiumbackend.shell.ir.CommandIR;
import net.magnesiumbackend.shell.ir.ExecutionMode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

class ShellInputSourceTest {

    @Test
    void constructorCreatesSource() {
        Map<String, CommandIR> commands = new HashMap<>();
        ShellEngine engine = new ShellEngine(name -> null);
        ShellInputSource source = new ShellInputSource(commands, engine);

        assertEquals("shell-stdin", source.name());
    }

    @Test
    void customPromptConstructor() {
        Map<String, CommandIR> commands = new HashMap<>();
        ShellEngine engine = new ShellEngine(name -> null);
        ShellInputSource source = new ShellInputSource(commands, engine, "> ");

        assertEquals("shell-stdin", source.name());
    }

    @Test
    void startAndStop() {
        Map<String, CommandIR> commands = new HashMap<>();
        ShellEngine engine = new ShellEngine(name -> null);
        ShellInputSource source = new ShellInputSource(commands, engine);

        source.start();
        source.stop();
    }
}
