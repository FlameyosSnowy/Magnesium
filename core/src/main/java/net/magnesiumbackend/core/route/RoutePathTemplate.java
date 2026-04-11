/*
package net.magnesiumbackend.core.route;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

*/
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
 *//*

public final class PathTemplate {

    private final String    raw;
    private final int       variableCount;

    private final String[] literals;      // null where variable
    private final String[] varNames;      // null where literal
    private final int segmentCount;

    public PathTemplate(String template, String[] literals, String[] varNames, int size) {

        this.raw = template;
        this.variableCount = varNames.length;
        this.literals = literals;
        this.varNames = varNames;
        this.segmentCount = size;
    }

    @Contract("_, _, _ -> new")
    public static @NotNull PathTemplate precompiled(
        String raw,
        String[] literals,
        String[] varNames
    ) {
        if (literals.length != varNames.length) {
            throw new IllegalArgumentException("Invalid precompiled template");
        }

        return new PathTemplate(raw, literals, varNames, literals.length);
    }

    */
/**
     * Parses {@code template} and returns a cached, immutable {@link PathTemplate}.
     *
     * @param template the route template, e.g. {@code /users/{id}}
     * @throws IllegalArgumentException if the template is blank or does not start with {@code /}
     *//*

    public static PathTemplate compile(String template) {
        if (template == null || template.isBlank()) {
            throw new IllegalArgumentException("Path template must not be blank.");
        }
        if (template.charAt(0) != '/') {
            throw new IllegalArgumentException("Path must start with '/'");
        }

        List<String> literals = new ArrayList<>();
        List<String> varNames = new ArrayList<>();

        int start = 0;
        for (int i = 0; i <= template.length(); i++) {
            if (i == template.length() || template.charAt(i) == '/') {
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

        return new PathTemplate(
            template,
            literals.toArray(new String[0]),
            varNames.toArray(new String[0]),
            literals.size()
        );
    }

    public String[] varNames() {
        return varNames;
    }

    public int variableCount() {
        return variableCount;
    }

    public String[] literals() {
        return literals;
    }

    public int segmentCount() {
        return segmentCount;
    }

    */
/**
     * Returns {@code true} if this template contains no variable segments,
     * i.e. it is a plain static route and can be looked up with a direct map get.
     *//*

    public boolean isStatic() {
        return variableCount == 0;
    }

    */
/** Returns the raw template string this instance was compiled from. *//*

    public String raw() {
        return raw;
    }

    @Override
    public String toString() {
        return "PathTemplate{" + raw + "}";
    }
}*/

package net.magnesiumbackend.core.route;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses and matches URL route templates of the form {@code /users/{id}/orders/{orderId}}.
 *
 * <h2>Compile-time use</h2>
 * Call the static helpers {@link #extractVariableNames(String)} and
 * {@link #extractVariableIndices(String)} to pull metadata out of a template
 * literal inside an annotation processor, no instance needed.
 *
 * <h2>Runtime use</h2>
 * Construct one {@link RoutePathTemplate} per registered route (done once at startup).
 * The instance pre-computes and caches the segment array and variable positions so
 * that {@link #match(String)} is a simple array walk with no string allocation
 * beyond the result map.
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
public final class RoutePathTemplate {

    private final int variableCount;
    private final String    raw;
    private final String[] literals;      // null where variable
    private final String[] varNames;      // null where literal
    private final int segmentCount;

    public RoutePathTemplate(String raw, String[] literals, String[] varNames, int segmentCount) {
        this.raw = raw;
        this.literals = literals;
        this.varNames = varNames;
        this.segmentCount = segmentCount;
        int count = 0;
        for (String v : varNames) {
            if (v != null) count++;
        }
        this.variableCount = count;
    }

    public static RoutePathTemplate precompiled(
        String raw,
        String[] literals,
        String[] varNames
    ) {
        return new RoutePathTemplate(raw, literals, varNames, literals.length);
    }

    /**
     * Parses {@code template} and returns a cached, immutable {@link RoutePathTemplate}.
     *
     * @param template the route template, e.g. {@code /users/{id}}
     * @throws IllegalArgumentException if the template is blank or does not start with {@code /}
     */
    public static RoutePathTemplate compile(String template) {
        if (template == null || template.isBlank()) {
            throw new IllegalArgumentException("Path template must not be blank.");
        }
        if (template.charAt(0) != '/') {
            throw new IllegalArgumentException("Path must start with '/'");
        }

        List<String> literals = new ArrayList<>(4);
        List<String> varNames = new ArrayList<>(4);

        int start = 1;
        for (int i = 1; i <= template.length(); i++) {
            if (i == template.length() || template.charAt(i) == '/') {
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

        return new RoutePathTemplate(
            template,
            literals.toArray(new String[0]),
            varNames.toArray(new String[0]),
            literals.size()
        );
    }

    @Nullable
    public Map<String, String> match(String path) {

        int len = path.length();
        int segIndex = 0;
        int start = 0;

        // Pre-size arrays for variable positions
        int[] varStarts = null;
        int[] varEnds   = null;
        int varCount = 0;

        for (int i = 0; i <= len; i++) {
            if (i == len || path.charAt(i) == '/') {

                if (segIndex >= segmentCount) return null;

                String literal = literals[segIndex];

                if (literal != null) {
                    // compare literal without substring
                    int segLen = i - start;
                    if (literal.length() != segLen) return null;

                    for (int j = 0; j < segLen; j++) {
                        if (path.charAt(start + j) != literal.charAt(j)) {
                            return null;
                        }
                    }
                } else {
                    // variable segment
                    if (varStarts == null) {
                        varStarts = new int[segmentCount];
                        varEnds   = new int[segmentCount];
                    }
                    varStarts[varCount] = start;
                    varEnds[varCount]   = i;
                    varCount++;
                }

                segIndex++;
                start = i + 1;
            }
        }

        if (segIndex != segmentCount) return null;

        // No variables → return shared empty map
        if (varCount == 0) {
            return Collections.emptyMap();
        }

        // Build result (only now allocate strings)
        Map<String, String> result = new HashMap<>(varCount * 2);

        int varIdx = 0;
        for (int i = 0; i < segmentCount; i++) {
            if (literals[i] == null) {
                String name = varNames[i];
                int s = varStarts[varIdx];
                int e = varEnds[varIdx];
                result.put(name, path.substring(s, e)); // allocation happens HERE only
                varIdx++;
            }
        }

        return result;
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

    /**
     * Extracts variable names from a template string in order.
     *
     * <p>Safe to call from an annotation processor, no {@link RoutePathTemplate}
     * instance is created.
     *
     * <pre>{@code
     * extractVariableNames("/users/{id}/orders/{orderId}")
     * // → ["id", "orderId"]
     * }</pre>
     */
    public static List<String> extractVariableNames(String template) {
        String[] parts = template.split("/", -1);
        List<String> names = new ArrayList<>();
        for (String p : parts) {
            if (!p.isEmpty() && p.charAt(0) == '{' && p.charAt(p.length() - 1) == '}' && p.length() > 2) {
                names.add(p.substring(1, p.length() - 1));
            }
        }
        return Collections.unmodifiableList(names);
    }

    /**
     * Extracts the segment indices at which variables appear.
     *
     * <p>Safe to call from an annotation processor.
     *
     * <pre>{@code
     * extractVariableIndices("/users/{id}/orders/{orderId}")
     * // → [2, 4]   (0-based, first element is the empty string before the leading '/')
     * }</pre>
     */
    public static int[] extractVariableIndices(String template) {
        String[] parts = template.split("/", -1);
        int[] indices = new int[parts.length];
        int count = 0;
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (!p.isEmpty() && p.charAt(0) == '{' && p.charAt(p.length() - 1) == '}' && p.length() > 2) {
                indices[count++] = i;
            }
        }
        int[] result = new int[count];
        System.arraycopy(indices, 0, result, 0, count);
        return result;
    }

    @Override
    public String toString() {
        return "PathTemplate{" + raw + "}";
    }

    public int variableCount() {
        return variableCount;
    }

    public String[] literals() {
        return literals;
    }

    public int segmentCount() {
        return segmentCount;
    }

    public String[] varNames() {
        return varNames;
    }
}