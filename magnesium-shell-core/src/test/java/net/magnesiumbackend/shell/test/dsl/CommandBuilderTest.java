package net.magnesiumbackend.shell.test.dsl;

import net.magnesiumbackend.shell.dsl.CommandBuilder;
import net.magnesiumbackend.shell.dsl.CommandContext;
import net.magnesiumbackend.shell.ir.CommandIR;
import net.magnesiumbackend.shell.ir.ExecutionMode;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class CommandBuilderTest {

    @Test
    void builderCreatesCommand() {
        CommandIR ir = CommandBuilder.create("test")
            .action(ctx -> {})
            .buildIR("TestHandler", "run");

        assertEquals("test", ir.name());
    }

    @Test
    void builderWithArgument() {
        CommandIR ir = CommandBuilder.create("test")
            .arg("name", String.class)
            .add()
            .action(ctx -> {})
            .buildIR("TestHandler", "run");

        assertTrue(ir.arguments().has("name"));
        assertEquals(String.class, ir.arguments().get("name").type());
    }

    @Test
    void builderWithRequiredArgument() {
        CommandIR ir = CommandBuilder.create("test")
            .arg("token", String.class)
            .required()
            .add()
            .action(ctx -> {})
            .buildIR("TestHandler", "run");

        assertTrue(ir.arguments().get("token").required());
    }

    @Test
    void builderWithDefaultValue() {
        CommandIR ir = CommandBuilder.create("test")
            .arg("port", Integer.class)
            .defaultValue(8080)
            .add()
            .action(ctx -> {})
            .buildIR("TestHandler", "run");

        assertEquals(8080, ir.arguments().get("port").defaultValue());
    }

    @Test
    void builderWithAmqpMode() {
        CommandIR ir = CommandBuilder.create("test")
            .mode(ExecutionMode.AMQP)
            .action(ctx -> {})
            .buildIR("TestHandler", "run");

        assertEquals(ExecutionMode.AMQP, ir.executionMode());
    }

    @Test
    void builderWithSyncMode() {
        CommandIR ir = CommandBuilder.create("test")
            .mode(ExecutionMode.LOCAL)
            .action(ctx -> {})
            .buildIR("TestHandler", "run");

        assertEquals(ExecutionMode.LOCAL, ir.executionMode());
    }

    @Test
    void builderDefaultIsSync() {
        CommandIR ir = CommandBuilder.create("test")
            .action(ctx -> {})
            .buildIR("TestHandler", "run");

        assertEquals(ExecutionMode.LOCAL, ir.executionMode());
    }

    @Test
    void builderChaining() {
        CommandIR ir = CommandBuilder.create("deploy")
            .arg("env", String.class)
            .defaultValue("dev")
            .add()
            .arg("version", String.class)
            .required()
            .add()
            .arg("force", Boolean.class)
            .defaultValue(false)
            .add()
            .mode(ExecutionMode.LOCAL)
            .action(ctx -> {})
            .buildIR("DeployHandler", "run");

        assertEquals("deploy", ir.name());
        assertEquals(3, ir.arguments().all().size());
        assertTrue(ir.arguments().has("env"));
        assertTrue(ir.arguments().has("version"));
        assertTrue(ir.arguments().has("force"));
    }

    @Test
    void actionIsStored() {
        CommandBuilder builder = CommandBuilder.create("test")
            .action(ctx -> {});

        assertNotNull(builder.action());
    }

    @Test
    void handlerReceivesContext() {
        CommandBuilder builder = CommandBuilder.create("greet")
            .arg("name", String.class)
            .add()
            .action(ctx -> ctx.put("greet", "Hello, " + ctx.get("name")));

        CommandContext context = new CommandContext("greet", new HashMap<>());
        context.put("name", "World");

        builder.action().accept(context);

        assertEquals("Hello, World", context.get("greet"));
    }
}