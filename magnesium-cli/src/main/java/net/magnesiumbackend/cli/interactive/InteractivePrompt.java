package net.magnesiumbackend.cli.interactive;

import org.fusesource.jansi.Ansi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Interactive prompt utilities for CLI.
 *
 * <p>Provides styled prompts with validation and selection interfaces.</p>
 */
public final class InteractivePrompt {
    private static final Logger log = LoggerFactory.getLogger(InteractivePrompt.class);

    private final BufferedReader reader;

    public InteractivePrompt() {
        this.reader = new BufferedReader(new InputStreamReader(System.in));
    }

    /**
     * Prompts for a string value.
     *
     * @param label the prompt label
     * @param defaultValue default value if user presses enter
     * @param validator validation predicate
     * @return the entered value
     */
    public @NotNull String prompt(
        @NotNull String label,
        @Nullable String defaultValue,
        @NotNull Predicate<String> validator
    ) throws IOException {
        while (true) {
            log.info(String.valueOf(Ansi.ansi()
                .fg(Ansi.Color.CYAN)
                .a(label)
                .reset()
                .a(defaultValue != null ? " [" + defaultValue + "]: " : ": ")));

            String line = reader.readLine();
            if (line == null) {
                throw new IOException("EOF while reading input");
            }

            line = line.trim();

            if (line.isEmpty() && defaultValue != null) {
                return defaultValue;
            }

            if (line.isEmpty()) {
                log.info(String.valueOf(Ansi.ansi()
                    .fg(Ansi.Color.RED)
                    .a("  ✗ This field is required")
                    .reset()));
                continue;
            }

            if (!validator.test(line)) {
                log.info(String.valueOf(Ansi.ansi()
                    .fg(Ansi.Color.RED)
                    .a("  ✗ Invalid input")
                    .reset()));
                continue;
            }

            return line;
        }
    }

    /**
     * Prompts with a default value (no custom validator).
     */
    public @NotNull String prompt(@NotNull String label, @Nullable String defaultValue) throws IOException {
        return prompt(label, defaultValue, s -> true);
    }

    /**
     * Prompts without a default value.
     */
    public @NotNull String prompt(@NotNull String label) throws IOException {
        return prompt(label, null, s -> !s.isEmpty());
    }

    /**
     * Shows a multi-select checklist.
     *
     * @param label the section label
     * @param options available options
     * @param defaults initially selected indices
     * @return list of selected option names
     */
    public @NotNull List<String> checklist(
        @NotNull String label,
        @NotNull List<String> options,
        boolean @NotNull [] defaults
    ) throws IOException {
        if (options.size() != defaults.length) {
            throw new IllegalArgumentException("Options and defaults must have same size");
        }

        boolean[] selected = defaults.clone();

        log.info(String.valueOf(Ansi.ansi()
            .fg(Ansi.Color.YELLOW)
            .a("\n" + label + ":")
            .reset())
        );
        log.info("  (space to toggle, enter to confirm)");

        // In a real implementation, this would use a terminal UI library
        // For now, we use a simple numbered prompt
        for (int i = 0; i < options.size(); i++) {
            String marker = selected[i] ? "[x]" : "[ ]";
            log.info("  {} {}. {}", marker, i + 1, options.get(i));
        }

        System.out.print(Ansi.ansi()
            .fg(Ansi.Color.CYAN)
            .a("Select numbers (comma-separated) or press enter to accept defaults: ")
            .reset()
        );
        System.out.flush();

        String line = reader.readLine();
        if (line == null) {
            line = "";
        }

        line = line.trim();

        if (!line.isEmpty()) {
            // Parse selections
            String[] parts = line.split(",");
            for (int i = 0; i < selected.length; i++) {
                selected[i] = false;
            }
            for (String part : parts) {
                try {
                    int idx = Integer.parseInt(part.trim()) - 1;
                    if (idx >= 0 && idx < options.size()) {
                        selected[idx] = true;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        List<String> result = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            if (selected[i]) {
                result.add(options.get(i));
            }
        }
        return result;
    }

    /**
     * Shows a single-select radio list.
     *
     * @param label the section label
     * @param options available options
     * @param defaultIndex initially selected index
     * @return selected option name
     */
    public @NotNull String radio(
        @NotNull String label,
        @NotNull List<String> options,
        int defaultIndex
    ) throws IOException {
        log.info(String.valueOf(Ansi.ansi()
            .fg(Ansi.Color.YELLOW)
            .a("\n" + label + ":")
            .reset())
        );

        for (int i = 0; i < options.size(); i++) {
            String marker = (i == defaultIndex) ? "(x)" : "( )";
            log.info("  {} {}. {}", marker, i + 1, options.get(i));
        }

        System.out.print(Ansi.ansi()
            .fg(Ansi.Color.CYAN)
            .a("Select number [" + (defaultIndex + 1) + "]: ")
            .reset()
        );
        System.out.flush();

        String line = reader.readLine();
        if (line == null || line.trim().isEmpty()) {
            return options.get(defaultIndex);
        }

        try {
            int idx = Integer.parseInt(line.trim()) - 1;
            if (idx >= 0 && idx < options.size()) {
                return options.get(idx);
            }
        } catch (NumberFormatException ignored) {
        }

        return options.get(defaultIndex);
    }

    /**
     * Confirms a yes/no question.
     *
     * @param label the question
     * @param defaultYes default if user presses enter
     * @return true if yes
     */
    public boolean confirm(@NotNull String label, boolean defaultYes) throws IOException {
        String defaultStr = defaultYes ? "Y/n" : "y/N";

        log.info(String.valueOf(Ansi.ansi()
            .fg(Ansi.Color.CYAN)
            .a(label + " [" + defaultStr + "]: ")
            .reset())
        );

        String line = reader.readLine();
        if (line == null || line.trim().isEmpty()) {
            return defaultYes;
        }

        String lower = line.trim().toLowerCase();
        return lower.equals("y") || lower.equals("yes");
    }

    /**
     * Prints a success message.
     */
    public void success(@NotNull String message) {
        log.info(String.valueOf(Ansi.ansi()
            .fg(Ansi.Color.GREEN)
            .a("✓ ")
            .reset()
            .a(message))
        );
    }

    /**
     * Prints an info message.
     */
    public void info(@NotNull String message) {
        log.info(String.valueOf(Ansi.ansi()
            .fg(Ansi.Color.BLUE)
            .a("ℹ ")
            .reset()
            .a(message))
        );
    }

    /**
     * Prints a warning message.
     */
    public void warning(@NotNull String message) {
        log.info(String.valueOf(Ansi.ansi()
            .fg(Ansi.Color.YELLOW)
            .a("⚠ ")
            .reset()
            .a(message))
        );
    }

    /**
     * Prints an error message.
     */
    public void error(@NotNull String message) {
        log.info(String.valueOf(Ansi.ansi()
            .fg(Ansi.Color.RED)
            .a("✗ ")
            .reset()
            .a(message))
        );
    }
}
