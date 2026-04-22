package net.magnesiumbackend.cli.commands;

import net.magnesiumbackend.cli.MagnesiumCli;
import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Dev mode command - starts with hot reload and enhanced logging.
 *
 * <pre>{@code
 * magnesium-cli dev
 * }</pre>
 */
public final class DevCommand implements MagnesiumCli.CliCommand {

    @Override
    public int execute(@NotNull String @NotNull [] args, @NotNull Path workingDir) {
        System.out.println(Ansi.ansi()
            .fg(Ansi.Color.GREEN)
            .a("Starting Magnesium in DEV mode...")
            .reset()
        );

        boolean isMaven = Files.exists(workingDir.resolve("pom.xml"));
        boolean isGradle = Files.exists(workingDir.resolve("build.gradle"));

        if (!isMaven && !isGradle) {
            System.err.println("No build file found");
            return 1;
        }

        ProcessBuilder pb;
        if (isMaven) {
            pb = new ProcessBuilder("mvn", "magnesium-devtools:run");
        } else {
            pb = new ProcessBuilder("./gradlew", "run", "-Pmagnesium.dev=true");
        }

        pb.directory(workingDir.toFile());
        pb.inheritIO();
        pb.environment().put("MAGNESIUM_DEV_MODE", "true");
        pb.environment().put("MAGNESIUM_SHELL_ENABLED", "true");
        pb.environment().put("LOG_LEVEL", "DEBUG");

        try {
            Process process = pb.start();
            return process.waitFor();
        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to start: " + e.getMessage());
            return 1;
        }
    }

    @Override
    public @NotNull String description() {
        return "Run in development mode with hot reload";
    }
}
