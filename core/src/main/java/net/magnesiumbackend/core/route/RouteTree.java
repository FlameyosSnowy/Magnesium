package net.magnesiumbackend.core.route;

import net.magnesiumbackend.core.headers.HttpPathParamIndex;
import net.magnesiumbackend.core.headers.Slice;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class RouteTree<T> {

    private final RouteNode<T> root = new RouteNode<>();

    private volatile List<RouteEntry<T>>    cachedEntries = null;
    private volatile List<RouteDumpEntry<T>> cachedDump   = null;

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
                throw new IllegalStateException(
                    "Invalid template at index " + i + " for route: " + template.raw());
            }
        }

        if (current.handler != null) {
            throw new IllegalStateException("Duplicate route: " + template.raw());
        }

        current.handler = handler;
        invalidateCache();
    }

    public RouteMatch<T> match(byte[] pathBytes) {
        RouteNode<T> current = root;

        int varCount  = 0;
        Slice[] varNames  = null;
        Slice[] varValues = null;

        int pos    = 0;
        int length = pathBytes.length;

        if (pos < length && pathBytes[pos] == '/') pos++;

        while (pos < length) {
            int segStart = pos;
            while (pos < length && pathBytes[pos] != '/') pos++;
            int segLen = pos - segStart;

            if (pos < length) pos++;

            RouteNode<T> next = null;

            byte firstByte = pathBytes[segStart];

            Slice[]       keys     = current.staticKeys;
            RouteNode<T>[] children = current.staticChildren;
            int[]          fbIdx   = current.firstByteIndex;

            int probe = fbIdx[firstByte & 0xFF];
            if (probe >= 0) {
                int keysLen = keys.length;
                for (int i = probe; i < keysLen; i++) {
                    Slice key = keys[i];
                    if (key == null || key.src()[key.start()] != firstByte) break;

                    if (sliceEqualsSegment(key, pathBytes, segStart, segLen)) {
                        next = children[i];
                        break;
                    }
                }
            }

            if (next != null) {
                current = next;
            } else if (current.variableChild != null) {
                if (varNames == null) {
                    varNames  = new Slice[8];
                    varValues = new Slice[8];
                } else if (varCount == varNames.length) {
                    int newSize = varNames.length * 2;
                    varNames  = Arrays.copyOf(varNames,  newSize);
                    varValues = Arrays.copyOf(varValues, newSize);
                }

                varNames[varCount]  = current.variableChild.variableName;
                varValues[varCount] = new Slice(pathBytes, segStart, segLen);
                varCount++;

                current = current.variableChild;
            } else if (current.wildcardChild != null) {
                throw new UnsupportedOperationException(
                    "Wildcard routes are not yet supported");
            } else {
                return null; // no match
            }
        }

        if (current.handler == null) return null;

        HttpPathParamIndex vars =
            varCount == 0
                ? HttpPathParamIndex.empty()
                : HttpPathParamIndex.of(varNames, varValues);

        return new RouteMatch<>(current.handler, vars);
    }

    private static boolean sliceEqualsSegment(
        Slice key, byte[] pathBytes, int segStart, int segLen) {

        // hoist all field reads to locals before the loop
        int keyLen = key.length();
        if (keyLen != segLen) return false;

        byte[] keySrc    = key.src();
        int    keyOffset = key.start();

        for (int i = 0; i < keyLen; i++) {
            if (keySrc[keyOffset + i] != pathBytes[segStart + i]) return false;
        }
        return true;
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
        cachedDump    = null;
    }

    @SuppressWarnings("unchecked")
    public static final class RouteNode<T> {

        Slice[]        staticKeys     = new Slice[0];
        RouteNode<T>[] staticChildren = (RouteNode<T>[]) new RouteNode[0];

        int[] firstByteIndex = new int[256];

        RouteNode<T> variableChild;
        Slice        variableName;

        RouteNode<T> wildcardChild;
        Slice        wildcardName;

        T handler;

        RouteNode() {
            Arrays.fill(firstByteIndex, -1);
        }

        RouteNode<T> addStaticChild(Slice key) {
            int length = staticKeys.length;
            for (int i = 0; i < length; i++) {
                if (key.equals(staticKeys[i])) return staticChildren[i];
            }

            // grow arrays
            int newLen = Math.max(length + 4, length == 0 ? 4 : length * 2);
            staticKeys     = Arrays.copyOf(staticKeys,     newLen);
            staticChildren = Arrays.copyOf(staticChildren, newLen);

            RouteNode<T> child = new RouteNode<>();
            staticKeys[length]     = key;
            staticChildren[length] = child;

            insertSorted(length);

            return child;
        }

        /**
         * Insertion-sort the newly added entry (at logical index {@code newIdx})
         * into first-byte order and rebuild {@code firstByteIndex}.
         *
         * Called only during registration (cold path) — no hot-path cost.
         */
        private void insertSorted(int newIdx) {
            int count = newIdx + 1; // number of live entries after the add

            // insertion-sort the live portion by first byte of key
            for (int i = count - 1; i > 0; i--) {
                Slice a = staticKeys[i - 1];
                Slice b = staticKeys[i];
                if (a == null || b == null) break;
                if ((a.src()[a.start()] & 0xFF) <= (b.src()[b.start()] & 0xFF)) break;

                // swap keys
                staticKeys[i - 1] = b;
                staticKeys[i]     = a;
                // swap children
                RouteNode<T> tmp       = staticChildren[i - 1];
                staticChildren[i - 1]  = staticChildren[i];
                staticChildren[i]      = tmp;
            }

            // rebuild firstByteIndex over the live portion
            Arrays.fill(firstByteIndex, -1);
            for (int i = 0; i < count; i++) {
                Slice k = staticKeys[i];
                if (k == null) continue;
                int fb = k.src()[k.start()] & 0xFF;
                if (firstByteIndex[fb] == -1) firstByteIndex[fb] = i;
            }
        }

        RouteNode<T> addVariableChild(Slice name) {
            if (variableChild == null) {
                variableChild          = new RouteNode<>();
                variableChild.variableName = name;
            }
            return variableChild;
        }

        RouteNode<T> addWildcardChild(Slice name) {
            if (wildcardChild == null) {
                wildcardChild              = new RouteNode<>();
                wildcardChild.wildcardName = name;
            }
            return wildcardChild;
        }
    }

    public record RouteDumpEntry<T>(String path, T handler) {}

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
        List<RouteDumpEntry<T>> out) {

        if (node.handler != null) {
            out.add(new RouteDumpEntry<>(render(path), node.handler));
        }

        Slice[]        staticKeys = node.staticKeys;
        RouteNode<T>[] children   = node.staticChildren;
        int length = staticKeys.length;
        for (int i = 0; i < length; i++) {
            Slice key = staticKeys[i];
            if (key == null) continue;

            path.add(key);
            dumpRecursive(children[i], path, out);
            path.removeLast();
        }

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