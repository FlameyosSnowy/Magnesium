package net.magnesiumbackend.cli.commands;

import net.magnesiumbackend.cli.MagnesiumCli;
import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Build command - compiles and packages the application.
 *
 * <pre>{@code
 * magnesium-cli build
 * magnesium-cli build --native
 * }</pre>
 */
public final class BuildCommand implements MagnesiumCli.CliCommand {

    @Override
    public int execute(@NotNull String @NotNull [] args, @NotNull Path workingDir) {
        boolean nativeBuild = false;
        for (String arg : args) {
            if (arg.equals("--native") || arg.equals("-n")) {
                nativeBuild = true;
            }
        }

        boolean isMaven = Files.exists(workingDir.resolve("pom.xml"));
        boolean isGradle = Files.exists(workingDir.resolve("build.gradle"));

        if (!isMaven && !isGradle) {
            System.err.println("No build file found");
            return 1;
        }

        if (nativeBuild) {
            System.out.println(Ansi.ansi()
                .fg(Ansi.Color.CYAN)
                .a("Building native image (requires GraalVM)...")
                .reset()
            );
        } else {
            System.out.println(Ansi.ansi()
                .fg(Ansi.Color.CYAN)
                .a("Building application...")
                .reset()
            );
        }

        ProcessBuilder pb;
        if (isMaven) {
            if (nativeBuild) {
                pb = new ProcessBuilder("mvn", "package", "-Pnative");
            } else {
                pb = new ProcessBuilder("mvn", "package");
            }
        } else {
            if (nativeBuild) {
                pb = new ProcessBuilder("./gradlew", "nativeCompile");
            } else {
                pb = new ProcessBuilder("./gradlew", "build");
            }
        }

        pb.directory(workingDir.toFile());
        pb.inheritIO();

        try {
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println(Ansi.ansi()
                    .fg(Ansi.Color.GREEN)
                    .a("✓ Build successful")
                    .reset()
                );
            } else {
                System.err.println(Ansi.ansi()
                    .fg(Ansi.Color.RED)
                    .a("✗ Build failed")
                    .reset()
                );
            }

            return exitCode;
        } catch (IOException | InterruptedException e) {
            System.err.println("Failed to build: " + e.getMessage());
            return 1;
        }
    }

    @Override
    public @NotNull String description() {
        return "Build and package the application";
    }
}
