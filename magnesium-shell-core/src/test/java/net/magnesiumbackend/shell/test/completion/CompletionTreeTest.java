package net.magnesiumbackend.shell.test.completion;

import net.magnesiumbackend.shell.completion.CompletionTree;
import net.magnesiumbackend.shell.ir.ArgumentSchema;
import net.magnesiumbackend.shell.ir.CommandIR;
import net.magnesiumbackend.shell.ir.ExecutionMode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Set;

class CompletionTreeTest {

    @Test
    void emptyTreeReturnsEmptyCompletions() {
        CompletionTree tree = new CompletionTree();
        List<String> completions = tree.complete("", 0);
        assertTrue(completions.isEmpty());
    }

    @Test
    void addSimpleCommand() {
        CompletionTree tree = new CompletionTree();
        CommandIR ir = createCommand("test");
        tree.addCommand(ir);

        Set<String> commands = tree.getAllCommands();
        assertTrue(commands.contains("test"));
    }

    @Test
    void completeSingleCommand() {
        CompletionTree tree = new CompletionTree();
        tree.addCommand(createCommand("deploy"));

        List<String> completions = tree.complete("de", 2);
        assertTrue(completions.contains("deploy"));
    }

    @Test
    void completeNamespacedCommand() {
        CompletionTree tree = new CompletionTree();
        tree.addCommand(createCommand("app:deploy"));
        tree.addCommand(createCommand("app:logs"));
        tree.addCommand(createCommand("db:migrate"));

        List<String> completions = tree.complete("app:", 4);
        System.out.println(completions);
        assertTrue(completions.contains("deploy"));
        assertTrue(completions.contains("logs"));
        assertFalse(completions.contains("migrate"));
    }

    @Test
    void completeWithArguments() {
        CompletionTree tree = new CompletionTree();
        ArgumentSchema schema = new ArgumentSchema.Builder()
            .arg("env", String.class)
                .add()
            .arg("force", Boolean.class)
                .add()
            .build();
        CommandIR ir = CommandIR.builder("deploy")
            .arguments(schema)
            .handlerClass("DeployHandler")
            .handlerMethod("execute")
            .executionMode(ExecutionMode.LOCAL)
            .build();
        tree.addCommand(ir);

        List<String> completions = tree.complete("deploy ", 7);
        assertTrue(completions.contains("--env"));
        assertTrue(completions.contains("--force"));
    }

    @Test
    void completePartialArgument() {
        CompletionTree tree = new CompletionTree();
        ArgumentSchema schema = new ArgumentSchema.Builder()
            .arg("environment", String.class)
                .add()
            .arg("verbose", Boolean.class)
                .add()
            .build();
        CommandIR ir = CommandIR.builder("run")
            .arguments(schema)
            .handlerClass("RunHandler")
            .handlerMethod("execute")
            .executionMode(ExecutionMode.LOCAL)
            .build();
        tree.addCommand(ir);

        List<String> completions = tree.complete("run --env", 9);
        assertTrue(completions.contains("--environment"));
    }

    @Test
    void completeAtCursorPosition() {
        CompletionTree tree = new CompletionTree();
        tree.addCommand(createCommand("deploy"));
        tree.addCommand(createCommand("destroy"));

        List<String> completions = tree.complete("depl", 2);
        assertTrue(completions.contains("deploy"));
        assertTrue(completions.contains("destroy"));
    }

    @Test
    void noMatchReturnsEmpty() {
        CompletionTree tree = new CompletionTree();
        tree.addCommand(createCommand("deploy"));

        List<String> completions = tree.complete("xyz", 3);
        assertTrue(completions.isEmpty());
    }

    @Test
    void getAllCommandsReturnsAll() {
        CompletionTree tree = new CompletionTree();
        tree.addCommand(createCommand("cmd1"));
        tree.addCommand(createCommand("cmd2"));
        tree.addCommand(createCommand("namespace:cmd3"));

        Set<String> commands = tree.getAllCommands();
        assertEquals(3, commands.size());
        assertTrue(commands.contains("cmd1"));
        assertTrue(commands.contains("cmd2"));
        assertTrue(commands.contains("namespace:cmd3"));
    }

    @Test
    void completeWithSpaceTokenization() {
        CompletionTree tree = new CompletionTree();
        tree.addCommand(createCommand("deploy app"));

        List<String> completions = tree.complete("deploy app", 10);
        assertTrue(completions.contains("app"));
    }

    @Test
    void hardCapAt64Completions() {
        CompletionTree tree = new CompletionTree();
        for (int i = 0; i < 100; i++) {
            tree.addCommand(createCommand("cmd" + i));
        }

        List<String> completions = tree.complete("cmd", 3);
        assertEquals(64, completions.size());
    }

    private CommandIR createCommand(String name) {
        return CommandIR.builder(name)
            .handlerClass("Handler")
            .handlerMethod("execute")
            .executionMode(ExecutionMode.LOCAL)
            .build();
    }
}
