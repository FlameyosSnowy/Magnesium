package net.magnesiumbackend.core.route;

import net.magnesiumbackend.core.headers.HttpPathParamIndex;
import net.magnesiumbackend.core.headers.Slice;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class RouteTree<T> {

    private final RouteNode<T> root = new RouteNode<>();

    private volatile List<RouteEntry<T>> cachedEntries = null;
    private volatile List<RouteDumpEntry<T>> cachedDump = null;

    public void register(RoutePathTemplate template, T handler) {
        RouteNode<T> current = root;

        Slice[] literals = template.literals();
        Slice[] varNames = template.varNames();

        int length = literals.length;

        for (int i = 0; i < length; i++) {
            Slice literal = literals[i];
            Slice varName = (varNames != null && i < varNames.length) ? varNames[i] : null;

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

    // ----------------------------
    // Match
    // ----------------------------

    public Optional<RouteMatch<T>> match(byte[] pathBytes) {
        RouteNode<T> current = root;

        int varCount = 0;
        Slice[] varNames = null;
        Slice[] varValues = null;

        int start = 0;
        int length = pathBytes.length;

        while (start < length) {

            if (pathBytes[start] == '/') {
                start++;
                continue;
            }

            int end = start;
            while (end < length && pathBytes[end] != '/') {
                end++;
            }

            int segmentLen = end - start;

            RouteNode<T> next = null;

            Slice segment = new Slice(pathBytes, start, segmentLen);

            Slice[] keys = current.staticKeys;
            RouteNode<T>[] children = current.staticChildren;

            int keysLength = keys.length;
            for (int i = 0; i < keysLength; i++) {
                Slice key = keys[i];
                if (key == null) continue;

                if (equals(key, segment)) {
                    next = children[i];
                    break;
                }
            }

            if (next != null) {
                current = next;
            } else if (current.variableChild != null) {

                if (varNames == null) {
                    varNames = new Slice[4];
                    varValues = new Slice[4];
                } else if (varCount == varNames.length) {
                    int newSize = varNames.length * 2;
                    varNames = Arrays.copyOf(varNames, newSize);
                    varValues = Arrays.copyOf(varValues, newSize);
                }

                varNames[varCount] = current.variableChild.variableName;
                varValues[varCount] = new Slice(pathBytes, start, segmentLen);
                varCount++;

                current = current.variableChild;
            } else {
                return Optional.empty();
            }

            start = end;
        }

        if (current.handler == null) return Optional.empty();

        HttpPathParamIndex vars =
            varCount == 0
                ? HttpPathParamIndex.empty()
                : HttpPathParamIndex.of(varNames, varValues);

        return Optional.of(new RouteMatch<>(current.handler, vars));
    }

    public record RouteMatch<T>(T handler, HttpPathParamIndex pathVariables) {}

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

    private List<RouteEntry<T>> buildEntries() {
        List<RouteEntry<T>> result = new ArrayList<>(16);
        buildRecursive(root, new ArrayList<>(32), result);
        return List.copyOf(result);
    }

    private void buildRecursive(RouteNode<T> node,
                                List<Slice> path,
                                List<RouteEntry<T>> out) {

        if (node.handler != null) {
            out.add(new RouteEntry<>(render(path), node.handler));
        }

        Slice[] staticKeys = node.staticKeys;
        int length = staticKeys.length;
        for (int i = 0; i < length; i++) {
            if (staticKeys[i] == null) continue;

            path.add(staticKeys[i]);
            buildRecursive(node.staticChildren[i], path, out);
            path.removeLast();
        }

        if (node.variableChild != null) {
            path.add(node.variableChild.variableName);
            buildRecursive(node.variableChild, path, out);
            path.removeLast();
        }
    }

    private static String toString(Slice s) {
        return new String(s.src(), s.start(), s.length(), StandardCharsets.UTF_8);
    }

    private void invalidateCache() {
        cachedEntries = null;
        cachedDump = null;
    }

    @SuppressWarnings("unchecked")
    public static final class RouteNode<T> {

        private Slice[] staticKeys = new Slice[0];
        private RouteNode<T>[] staticChildren = (RouteNode<T>[]) new RouteNode[0];

        private RouteNode<T> variableChild;
        private Slice variableName;

        private T handler;

        private RouteNode<T> addStaticChild(Slice key) {
            int length = staticKeys.length;
            for (int i = 0; i < length; i++) {
                if (key.equals(staticKeys[i])) {
                    return staticChildren[i];
                }
            }

            int max = Math.max(length + 4, length * 2);

            staticKeys = Arrays.copyOf(staticKeys, max);
            staticChildren = Arrays.copyOf(staticChildren, max);

            RouteNode<T> child = new RouteNode<>();

            staticKeys[length] = key;
            staticChildren[length] = child;

            return child;
        }

        private RouteNode<T> addVariableChild(Slice name) {
            if (variableChild == null) {
                variableChild = new RouteNode<>();
                variableChild.variableName = name;
            }
            return variableChild;
        }
    }

    private static boolean equals(Slice a, Slice b) {
        if (a.length() != b.length()) return false;

        byte[] ad = a.src();
        byte[] bd = b.src();

        int ao = a.start();
        int bo = b.start();
        int len = a.length();

        for (int i = 0; i < len; i++) {
            if (ad[ao + i] != bd[bo + i]) return false;
        }

        return true;
    }

    public record RouteDumpEntry<T>(
        String path,
        T handler
    ) {}

    public List<RouteDumpEntry<T>> dump() {
        List<RouteDumpEntry<T>> cached = cachedDump;
        if (cached != null) return cached;

        List<RouteDumpEntry<T>> out = new ArrayList<>(16);
        dumpRecursive(root, new ArrayList<>(32), out);
        cachedDump = out;
        return List.copyOf(out);
    }

    private void dumpRecursive(
        RouteNode<T> node,
        List<Slice> path,
        List<RouteDumpEntry<T>> out
    ) {
        if (node.handler != null) {
            out.add(new RouteDumpEntry<>(render(path), node.handler));
        }

        // static routes
        Slice[] staticKeys = node.staticKeys;
        int length = staticKeys.length;
        RouteNode<T>[] children = node.staticChildren;
        for (int i = 0; i < length; i++) {
            Slice key = staticKeys[i];
            if (key == null) continue;

            path.add(key);
            dumpRecursive(children[i], path, out);
            path.removeLast();
        }

        // variable route
        if (node.variableChild != null) {
            path.add(node.variableChild.variableName);
            dumpRecursive(node.variableChild, path, out);
            path.removeLast();
        }
    }

    private String render(List<Slice> segments) {
        if (segments.isEmpty()) return "/";

        StringBuilder sb = new StringBuilder(64);

        for (Slice s : segments) {
            sb.append('/').append(toString(s));
        }

        return sb.toString();
    }
}