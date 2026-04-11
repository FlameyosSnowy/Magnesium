package net.magnesiumbackend.processor.path;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses and matches URL route templates of the form {@code /users/{id}/orders/{orderId}}.
 *
 * <pre>{@code
 * // Startup
 * PathTemplate template = PathTemplate.compile("/users/{id}/orders/{orderId}");
 *
 * // Per-request
 * Map<String, String> vars = template.match(request.route());
 * if (vars != null) { ... }
 * }</pre>
 */
public final class CompiledPathTemplate {
    private final String    raw;
    private final int       variableCount;

    private final String[] literals;      // null where variable
    private final String[] varNames;      // null where literal
    private final int segmentCount;

    public CompiledPathTemplate(String template, String[] literals, String[] varNames, int size) {

        this.raw = template;
        this.variableCount = varNames.length;
        this.literals = literals;
        this.varNames = varNames;
        this.segmentCount = size;
    }

    /**
     * Parses {@code template} and returns a cached, immutable {@link net.magnesiumbackend.processor.path.CompiledPathTemplate}.
     *
     * @param template the route template, e.g. {@code /users/{id}}
     * @throws IllegalArgumentException if the template is blank or does not start with {@code /}
     */
    public static net.magnesiumbackend.processor.path.CompiledPathTemplate compile(String template) {
        if (template == null || template.isBlank()) {
            throw new IllegalArgumentException("Path template must not be blank.");
        }
        if (template.charAt(0) != '/') {
            throw new IllegalArgumentException("Path must start with '/'");
        }

        List<String> literals = new ArrayList<>(4);
        List<String> varNames = new ArrayList<>(4);

        int start = 1; // skip leading '/'

        for (int i = 1; i <= template.length(); i++) {
            if (i == template.length() || template.charAt(i) == '/') {

                if (i == start) { // skip empty segments
                    start = i + 1;
                    continue;
                }

                String segment = template.substring(start, i);

                if (segment.length() > 2 &&
                    segment.charAt(0) == '{' &&
                    segment.charAt(segment.length() - 1) == '}') {

                    literals.add(null);
                    varNames.add(segment.substring(1, segment.length() - 1));
                } else {
                    literals.add(segment);
                    varNames.add(null);
                }

                start = i + 1;
            }
        }

        return new net.magnesiumbackend.processor.path.CompiledPathTemplate(
            template,
            literals.toArray(new String[0]),
            varNames.toArray(new String[0]),
            literals.size()
        );
    }

    public int variableCount() {
        return variableCount;
    }

    public String[] literals() {
        return literals;
    }

    public String[] varNames() {
        return varNames;
    }

    public int segmentCount() {
        return segmentCount;
    }

    /**
     * Returns {@code true} if this template contains no variable segments,
     * i.e. it is a plain static route and can be looked up with a direct map get.
     */
    public boolean isStatic() {
        return variableCount == 0;
    }

    /** Returns the raw template string this instance was compiled from. */
    public String raw() {
        return raw;
    }

    @Override
    public String toString() {
        return "CompiledPathTemplate{" +
            "raw='" + raw + '\'' +
            ", variableCount=" + variableCount +
            ", literals=" + Arrays.toString(literals) +
            ", varNames=" + Arrays.toString(varNames) +
            ", segmentCount=" + segmentCount +
            '}';
    }
}