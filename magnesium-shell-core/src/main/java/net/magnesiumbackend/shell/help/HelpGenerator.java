package net.magnesiumbackend.shell.help;

import net.magnesiumbackend.shell.ir.ArgumentSchema;
import net.magnesiumbackend.shell.ir.CommandIR;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Generates help text for commands.
 *
 * <p>Compile-time generated help metadata for zero runtime cost.</p>
 */
public final class HelpGenerator {

    /**
     * Generates help text for a single command.
     *
     * @param ir the command IR
     * @return formatted help text
     */
    public @NotNull String generate(@NotNull CommandIR ir) {
        String description = ir.description();
        StringBuilder sb = new StringBuilder(
            64
                + ir.name().length() + (description == null ? 0 : description.length())
                + ir.arguments().all().size() * 16
                + ir.arguments().all().size() * 16);

        // Usage line
        sb.append("Usage: ").append(ir.name());

        for (ArgumentSchema.ArgumentDef arg : ir.arguments().all().values()) {
            if (arg.positional()) {
                if (arg.required()) {
                    sb.append(" <").append(arg.name()).append(">");
                } else {
                    sb.append(" [").append(arg.name()).append("]");
                }
            }
        }

        for (ArgumentSchema.ArgumentDef arg : ir.arguments().all().values()) {
            if (!arg.positional()) {
                sb.append(" [--").append(arg.name());
                if (!arg.flag()) {
                    sb.append("=<value>");
                }
                sb.append("]");
            }
        }

        sb.append("\n\n");

        // Description
        if (description != null && !description.isEmpty()) {
            sb.append(description).append("\n\n");
        }

        // Arguments
        if (!ir.arguments().all().isEmpty()) {
            sb.append("Arguments:\n");

            for (ArgumentSchema.ArgumentDef arg : ir.arguments().all().values()) {
                sb.append("  ");
                if (!arg.positional()) {
                    sb.append("--");
                }
                sb.append(arg.name());

                if (arg.required()) {
                    sb.append(" (required)");
                }
                if (arg.defaultValue() != null) {
                    sb.append(" (default: ").append(arg.defaultValue()).append(")");
                }

                sb.append("\n");

                if (!arg.description().isEmpty()) {
                    sb.append("    ").append(arg.description()).append("\n");
                }
            }
        }

        return sb.toString();
    }

    /**
     * Generates help for all commands.
     *
     * @param commands map of command names to IR
     * @return formatted help text
     */
    public @NotNull String generateAll(@NotNull Map<String, CommandIR> commands) {
        StringBuilder sb = new StringBuilder();
        sb.append("Available Commands:\n\n");

        // Group by namespace
        Map<String, List<CommandIR>> byNamespace = new TreeMap<>();

        for (CommandIR cmd : commands.values()) {
            String ns = cmd.name().contains(":")
                ? cmd.name().substring(0, cmd.name().indexOf(':'))
                : "general";
            byNamespace.computeIfAbsent(ns, k -> new ArrayList<>()).add(cmd);
        }

        for (Map.Entry<String, List<CommandIR>> entry : byNamespace.entrySet()) {
            sb.append(entry.getKey()).append(":\n");

            for (CommandIR cmd : entry.getValue()) {
                sb.append("  ").append(cmd.name());
                if (cmd.description() != null && !cmd.description().isEmpty()) {
                    String desc = cmd.description();
                    if (desc.length() > 50) {
                        desc = desc.substring(0, 47) + "...";
                    }
                    sb.append(" - ").append(desc);
                }
                sb.append("\n");
            }

            sb.append("\n");
        }

        return sb.toString();
    }
}
