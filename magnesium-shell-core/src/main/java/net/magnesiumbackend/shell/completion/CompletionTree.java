package net.magnesiumbackend.shell.completion;

import net.magnesiumbackend.shell.ir.ArgumentSchema;
import net.magnesiumbackend.shell.ir.CommandIR;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * High-performance static completion tree.
 * Zero regex, minimal allocations in hot path.
 */
public final class CompletionTree {

    private final Node root = new Node("");

    // reusable buffer for completions (thread-unsafe but CLI usually single-threaded)
    private final ArrayList<String> buffer = new ArrayList<>(16);

    public void addCommand(@NotNull CommandIR ir) {
        String name = ir.name();

        Node current = root;

        int start = 0;
        for (int i = 0; i <= name.length(); i++) {
            if (i == name.length() || name.charAt(i) == ':' || name.charAt(i) == ' ') {
                String part = name.substring(start, i);
                current = current.getOrCreate(part);
                start = i + 1;
            }
        }

        for (ArgumentSchema.ArgumentDef arg : ir.arguments().all().values()) {
            current.getOrCreate("--" + arg.name());
        }
        current.isCommand = true;
        current.command = ir;
    }

    public @NotNull List<String> complete(@NotNull String prefix, int cursor) {
        buffer.clear();

        Node current = root;

        int start = 0;
        int len = Math.min(cursor, prefix.length());

        String lastToken = "";

        for (int i = 0; i <= len; i++) {
            if (i == len || prefix.charAt(i) == ' ' || prefix.charAt(i) == ':') {

                if (start < i) {
                    String token = prefix.substring(start, i);

                    if (i == len) {
                        lastToken = token;
                        break;
                    }

                    Node next = current.get(token);
                    if (next == null) return List.of();
                    current = next;

                    start = i + 1;
                }
            }
        }

        if (lastToken.isEmpty() && start < len) {
            lastToken = prefix.substring(start, len);
        }

        int count = 0;
        for (Node child : current.children.values()) {
            if (lastToken.isEmpty() || child.name.startsWith(lastToken)) {
                buffer.add(child.name);
                if (++count == 64) break;
            }
        }

        return buffer.isEmpty() ? List.of() : new ArrayList<>(buffer);
    }

    public @NotNull Set<String> getAllCommands() {
        HashSet<String> out = new HashSet<>();
        collect(root, "", out);
        return out;
    }

    private void collect(Node node, String prefix, Set<String> out) {
        if (node.isCommand && node.command != null) {
            out.add(node.command.name());
        }

        for (Node child : node.children.values()) {
            collect(child, prefix, out);
        }
    }

    private static final class Node {
        final String name;
        final Map<String, Node> children = new HashMap<>(2, 1.0f);

        boolean isCommand;
        CommandIR command;

        Node(String name) {
            this.name = name;
        }

        Node get(String key) {
            return children.get(key);
        }

        Node getOrCreate(String key) {
            Node n = children.get(key);
            if (n == null) {
                n = new Node(key);
                children.put(key, n);
            }
            return n;
        }
    }
}