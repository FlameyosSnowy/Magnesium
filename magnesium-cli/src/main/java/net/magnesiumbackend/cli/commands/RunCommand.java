package net.magnesiumbackend.cli.commands;

import net.magnesiumbackend.cli.MagnesiumCli;
import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Run command - starts the application with shell attached.
 *
 * <pre>{@code
 * magnesium-cli run
 * magnesium-cli run --production
 * }</pre>
 */
public final class RunCommand implements MagnesiumCli.CliCommand {

    @Override
    public int execute(@NotNull String @NotNull [] args, @NotNull Path workingDir) {
        System.out.println(Ansi.ansi()
            .fg(Ansi.Color.CYAN)
            .a("Starting Magnesium application...")
            .reset()
        );

        // Detect build system
        boolean isMaven = Files.exists(workingDir.resolve("pom.xml"));
        boolean isGradle = Files.exists(workingDir.resolve("build.gradle"));

        if (!isMaven && !isGradle) {
            System.err.println("No build file found (pom.xml or build.gradle)");
            return 1;
        }

        ProcessBuilder pb;
        if (isMaven) {
            pb = new ProcessBuilder("mvn", "compile", "exec:java");
        } else {
            pb = new ProcessBuilder("./gradlew", "run");
        }

        pb.directory(workingDir.toFile());
        pb.inheritIO();

        // Enable shell by default
        pb.environment().put("MAGNESIUM_SHELL_ENABLED", "true");

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
        return "Run the application with shell attached";
    }
}
