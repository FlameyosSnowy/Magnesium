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

import net.magnesiumbackend.core.headers.Slice;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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
 */
public final class RoutePathTemplate {

    private final int variableCount;
    private final Slice raw;

    private final Slice[] literals;   // null where variable
    private final Slice[] varNames;   // null where literal
    private final int segmentCount;

    public RoutePathTemplate(Slice raw, Slice[] literals, Slice[] varNames, int segmentCount) {
        this.raw = raw;
        this.literals = literals;
        this.varNames = varNames;
        this.segmentCount = segmentCount;

        int count = 0;
        for (Slice v : varNames) {
            if (v != null) count++;
        }
        this.variableCount = count;
    }

    public static RoutePathTemplate compile(byte[] templateBytes) {
        if (templateBytes == null) {
            throw new IllegalArgumentException("Template must not be empty");
        }

        int length = templateBytes.length;
        if (length == 0) {
            throw new IllegalArgumentException("Template must not be empty");
        }

        if (templateBytes[0] != '/') {
            throw new IllegalArgumentException("Template must start with '/'");
        }

        List<Slice> literals = new ArrayList<>(4);
        List<Slice> varNames = new ArrayList<>(4);

        int start = 1;

        for (int i = 1; i <= length; i++) {
            if (i == length || templateBytes[i] == '/') {

                int len = i - start;

                if (len > 2 &&
                    templateBytes[start] == '{' &&
                    templateBytes[i - 1] == '}') {

                    literals.add(null);
                    varNames.add(new Slice(templateBytes, start + 1, len - 2));
                } else {
                    literals.add(new Slice(templateBytes, start, len));
                    varNames.add(null);
                }

                start = i + 1;
            }
        }

        Slice raw = new Slice(templateBytes, 0, length);

        return new RoutePathTemplate(
            raw,
            literals.toArray(new Slice[0]),
            varNames.toArray(new Slice[0]),
            literals.size()
        );
    }

    public boolean isStatic() {
        return variableCount == 0;
    }

    public Slice raw() {
        return raw;
    }

    public int variableCount() {
        return variableCount;
    }

    public Slice[] literals() {
        return literals;
    }

    public Slice[] varNames() {
        return varNames;
    }

    public int segmentCount() {
        return segmentCount;
    }
}