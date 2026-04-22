package net.magnesiumbackend.shell.commands;

import net.magnesiumbackend.shell.annotation.Command;
import net.magnesiumbackend.shell.annotation.Arg;
import net.magnesiumbackend.shell.dsl.CommandContext;
import net.magnesiumbackend.shell.ir.ExecutionMode;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Execute arbitrary system commands from the shell.
 *
 * <pre>{@code
 * > exec ls -la
 * > exec cat application.toml
 * > exec curl http://localhost:8080/health
 * }</pre>
 */
@Command(value = "exec", mode = ExecutionMode.LOCAL)
public final class ExecCommand {

    public void execute(
        @Arg(value = "command", required = true, description = "Command to execute") String command,
        CommandContext ctx
    ) {
        if (command == null || command.isEmpty()) {
            System.err.println("Usage: exec <command> [args...]");
            System.err.println("Examples:");
            System.err.println("  exec ls -la");
            System.err.println("  exec ps aux | grep java");
            return;
        }

        // Collect remaining args
        StringBuilder fullCommand = new StringBuilder(command);
        for (int i = 1; ; i++) {
            String arg = ctx.argOrDefault(String.valueOf(i), null);
            if (arg == null) break;
            fullCommand.append(" ").append(arg);
        }

        String cmd = fullCommand.toString();
        System.out.println("$ " + cmd);
        System.out.println();

        try {
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", cmd);
            pb.inheritIO();
            Process process = pb.start();
            int exitCode = process.waitFor();

            System.out.println();
            if (exitCode != 0) {
                System.err.println("Exit code: " + exitCode);
            }
        } catch (Exception e) {
            System.err.println("Error executing command: " + e.getMessage());
        }
    }
}
