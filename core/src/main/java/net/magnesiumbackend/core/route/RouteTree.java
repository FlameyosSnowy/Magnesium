package net.magnesiumbackend.core.route;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RouteTree<T> {

    private final RouteNode<T> root = new RouteNode<>();

    private volatile List<RouteEntry<T>> cachedEntries = null;

    public void register(@NotNull RoutePathTemplate template, T handler) {
        RouteNode<T> current = root;
        String[] literals = template.literals();
        String[] varNames = template.varNames();

        int literalsLength = literals.length;
        for (int i = 0; i < literalsLength; i++) {
            String literal = literals[i];
            String varName = (varNames != null && i < varNames.length) ? varNames[i] : null;

            if (literal != null) {
                current = current.addStaticChild(literal);
            } else if (varName != null) {
                current = current.addVariableChild(varName);
            } else {
                throw new IllegalStateException("Invalid template at index " + i + " for route: " + template.raw());
            }
        }

        if (current.handler != null) {
            throw new IllegalStateException("Duplicate route: " + template.raw());
        }

        current.handler = handler;
        invalidateCache();
    }

    public boolean unregister(RoutePathTemplate template) {
        RouteNode<T> current = root;
        String[] literals = template.literals();
        String[] varNames = template.varNames();

        int literalsLength = literals.length;
        int varNamesLength = varNames.length;
        for (int i = 0; i < literalsLength; i++) {
            String literal = literals[i];
            String varName = i < varNamesLength ? varNames[i] : null;

            if (literal != null) {
                current = current.getStaticChild(literal);
            } else if (varName != null) {
                current = current.variableChild;
            }

            if (current == null) return false;
        }

        if (current.handler == null) return false;
        current.handler = null;
        invalidateCache();
        return true;
    }

    public Optional<RouteTree.RouteMatch<T>> match(CharSequence path) {
        RouteNode<T> current = root;
        int varCount = 0;
        String[] varNames = null;
        String[] varValues = null;

        int length = path.length();
        int start = 0;

        while (start < length) {
            // skip '/'
            if (path.charAt(start) == '/') {
                start++;
                continue;
            }

            int end = start;
            while (end < length && path.charAt(end) != '/') {
                end++;
            }

            RouteNode<T> next = null;
            RouteNode<T>[] children = current.staticChildren;
            int staticChildrenLength = children.length;
            String[] keys = current.staticKeys;
            for (int i = 0; i < staticChildrenLength; i++) {
                String key = keys[i];
                if (key == null) continue;

                int keyLength = key.length();
                if (keyLength == end - start) {
                    boolean match = true;
                    for (int j = 0; j < keyLength; j++) {
                        if (key.charAt(j) != path.charAt(start + j)) {
                            match = false;
                            break;
                        }
                    }
                    if (match) {
                        next = children[i];
                        break;
                    }
                }
            }

            if (next != null) {
                current = next;
            } else if (current.variableChild != null) {
                if (varNames == null) {
                    varNames = new String[4];
                    varValues = new String[4];
                } else if (varCount == varNames.length) {
                    int newSize = varNames.length * 2;
                    varNames = Arrays.copyOf(varNames, newSize);
                    varValues = Arrays.copyOf(varValues, newSize);
                }

                varNames[varCount] = current.variableChild.variableName;
                varValues[varCount] = path.subSequence(start, end).toString();
                varCount++;

                current = current.variableChild;
            } else {
                return Optional.empty();
            }

            start = end;
        }

        if (current.handler == null) return Optional.empty();

        Map<String, String> vars = Map.of();
        if (varCount > 0) {
            Map<String, String> m = new HashMap<>(varCount * 2);
            for (int i = 0; i < varCount; i++) {
                m.put(varNames[i], varValues[i]);
            }
            vars = Map.copyOf(m);
        }

        return Optional.of(new RouteTree.RouteMatch<>(current.handler, vars));
    }

    public record RouteMatch<T>(T handler, Map<String, String> pathVariables) {
    }

    @SuppressWarnings("unchecked")
    public static final class RouteNode<T> {
        private String[] staticKeys = new String[0];
        private RouteNode<T>[] staticChildren = (RouteNode<T>[]) new RouteNode[0];
        private RouteNode<T> variableChild;
        private String variableName;
        private T handler;

        private RouteNode<T> addStaticChild(String key) {
            int length = staticKeys.length;
            for (int i = 0; i < length; i++) {
                String staticKey = staticKeys[i];
                if (staticKey != null && staticKey.equals(key)) return staticChildren[i];
            }

            int max = Math.max(length + 4, length * 2);

            staticKeys = Arrays.copyOf(staticKeys, max);
            staticChildren = Arrays.copyOf(staticChildren, max);
            RouteNode<T> child = new RouteNode<>();
            staticKeys[length] = key;
            staticChildren[length] = child;
            return child;
        }

        private RouteNode<T> addVariableChild(String name) {
            if (variableChild == null) {
                variableChild = new RouteNode<>();
                variableChild.variableName = name;
            }
            return variableChild;
        }

        @Contract(pure = true)
        private @Nullable RouteNode<T> getStaticChild(String key) {
            String[] keys = staticKeys;
            RouteNode<T>[] children = staticChildren;
            int length = keys.length;
            for (int i = 0; i < length; i++) {
                String staticKey = keys[i];
                if (staticKey != null && staticKey.equals(key)) return children[i];
            }
            return null;
        }
    }

    public record RouteDumpEntry<T>(
        String path,
        T handler
    ) {}

    public List<RouteDumpEntry<T>> dump() {
        List<RouteDumpEntry<T>> result = new ArrayList<>(16);
        StringBuilder path = new StringBuilder(32);

        dumpRecursive(root, path, result);

        return List.copyOf(result);
    }

    private void dumpRecursive(
        RouteNode<T> node,
        StringBuilder path,
        java.util.List<RouteDumpEntry<T>> out
    ) {
        int originalLength = path.length();

        // If this node has a handler, record it
        T handler = node.handler;
        if (handler != null) {
            String finalPath = path.isEmpty() ? "/" : path.toString();
            out.add(new RouteDumpEntry<>(finalPath, handler));
        }

        // Static children
        String[] keys = node.staticKeys;
        int length = keys.length;
        RouteNode<T>[] children = node.staticChildren;
        for (int i = 0; i < length; i++) {
            String key = keys[i];
            RouteNode<T> child = children[i];

            if (key == null || child == null) continue;

            path.append('/').append(key);
            dumpRecursive(child, path, out);
            path.setLength(originalLength);
        }

        // Variable child
        RouteNode<T> variableChild = node.variableChild;
        if (variableChild != null) {
            path.append("/{").append(variableChild.variableName).append('}');
            dumpRecursive(variableChild, path, out);
            path.setLength(originalLength);
        }
    }


    public record RouteEntry<T>(String path, T handler) {}

    public List<RouteEntry<T>> entries() {
        List<RouteEntry<T>> cached = cachedEntries;
        if (cached != null) return cached;

        synchronized (this) {
            if (cachedEntries != null) return cachedEntries;
            cachedEntries = buildEntries();
            return cachedEntries;
        }
    }

    private void invalidateCache() {
        cachedEntries = null;
    }

    private List<RouteEntry<T>> buildEntries() {
        List<RouteEntry<T>> result = new ArrayList<>(16);
        buildEntriesRecursive(root, new StringBuilder(32), result);
        return List.copyOf(result);
    }

    private void buildEntriesRecursive(
        RouteNode<T> node,
        StringBuilder path,
        List<RouteEntry<T>> out
    ) {
        int originalLength = path.length();

        if (node.handler != null) {
            out.add(new RouteEntry<>(path.isEmpty() ? "/" : path.toString(), node.handler));
        }

        String[] keys = node.staticKeys;
        RouteNode<T>[] children = node.staticChildren;
        int length = keys.length;
        for (int i = 0; i < length; i++) {
            if (keys[i] == null || children[i] == null) continue;
            path.append('/').append(keys[i]);
            buildEntriesRecursive(children[i], path, out);
            path.setLength(originalLength);
        }

        if (node.variableChild != null) {
            path.append("/{").append(node.variableChild.variableName).append('}');
            buildEntriesRecursive(node.variableChild, path, out);
            path.setLength(originalLength);
        }
    }
}