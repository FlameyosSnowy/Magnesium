package net.magnesiumbackend.shell.test.ir;

import net.magnesiumbackend.shell.ir.ArgumentSchema;
import net.magnesiumbackend.shell.ir.CommandIR;
import net.magnesiumbackend.shell.ir.ExecutionMode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CommandIRTest {

    @Test
    void builderCreatesCommand() {
        CommandIR ir = CommandIR.builder("test")
            .handlerClass("TestHandler").handlerMethod("execute")
            .build();

        assertEquals("test", ir.name());
    }

    @Test
    void builderSetsSchema() {
        ArgumentSchema schema = new ArgumentSchema.Builder()
            .arg("arg", String.class)
                .add()
            .build();

        CommandIR ir = CommandIR.builder("test")
            .arguments(schema)
            .handlerClass("TestHandler").handlerMethod("execute")
            .build();

        assertSame(schema, ir.arguments());
    }

    @Test
    void builderSetsExecutionMode() {
        CommandIR ir = CommandIR.builder("test")
            .handlerClass("TestHandler").handlerMethod("execute")
            .executionMode(ExecutionMode.DATA)
            .build();

        assertEquals(ExecutionMode.DATA, ir.executionMode());
    }

    @Test
    void builderDefaultExecutionModeIsLocal() {
        CommandIR ir = CommandIR.builder("test")
            .handlerClass("TestHandler").handlerMethod("execute")
            .build();

        assertEquals(ExecutionMode.LOCAL, ir.executionMode());
    }

    @Test
    void builderSetsDescription() {
        CommandIR ir = CommandIR.builder("test")
            .description("Test command")
            .handlerClass("TestHandler").handlerMethod("execute")
            .build();

        assertEquals("Test command", ir.description());
    }

    @Test
    void builderSetsHandlerClassAndMethod() {
        CommandIR ir = CommandIR.builder("test")
            .handlerClass("MyHandler").handlerMethod("handleMethod")
            .build();

        assertEquals("MyHandler", ir.handlerClass());
        assertEquals("handleMethod", ir.handlerMethod());
    }

    @Test
    void builderDefaultHandlerMethod() {
        CommandIR ir = CommandIR.builder("test")
            .handlerClass("MyHandler")
            .build();

        assertEquals("execute", ir.handlerMethod());
    }

    @Test
    void builderSetsAmqpBinding() {
        CommandIR ir = CommandIR.builder("test")
            .handlerClass("TestHandler").handlerMethod("execute")
            .amqpBinding("cmd.test")
            .build();

        assertEquals("cmd.test", ir.amqpBinding());
    }

    @Test
    void builderAddsAliases() {
        CommandIR ir = CommandIR.builder("test")
            .handlerClass("TestHandler").handlerMethod("execute")
            .alias("t")
            .alias("tst")
            .build();

        assertTrue(ir.aliases().contains("t"));
        assertTrue(ir.aliases().contains("tst"));
    }

    @Test
    void equality() {
        CommandIR ir1 = CommandIR.builder("test")
            .handlerClass("Handler").handlerMethod("exec")
            .build();
        CommandIR ir2 = CommandIR.builder("test")
            .handlerClass("Handler").handlerMethod("exec")
            .build();

        assert ir1.equals(ir2);
        assertEquals(ir1.hashCode(), ir2.hashCode());
    }

    @Test
    void inequality() {
        CommandIR ir1 = CommandIR.builder("test1")
            .handlerClass("Handler").handlerMethod("exec")
            .build();
        CommandIR ir2 = CommandIR.builder("test2")
            .handlerClass("Handler").handlerMethod("exec")
            .build();

        assertNotEquals(ir1, ir2);
    }

    @Test
    void toStringContainsName() {
        CommandIR ir = CommandIR.builder("my-command")
            .handlerClass("Handler").handlerMethod("exec")
            .build();

        assertTrue(ir.toString().contains("my-command"));
    }

    @Test
    void allExecutionModes() {
        assertEquals(ExecutionMode.LOCAL, ExecutionMode.valueOf("LOCAL"));
        assertEquals(ExecutionMode.DATA, ExecutionMode.valueOf("DATA"));
        assertEquals(ExecutionMode.AMQP, ExecutionMode.valueOf("AMQP"));
        assertEquals(3, ExecutionMode.values().length);
    }
}
