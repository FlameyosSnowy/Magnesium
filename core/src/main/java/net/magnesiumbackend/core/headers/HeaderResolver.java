package net.magnesiumbackend.core.headers;

/**
 * Resolves HTTP header names to their {@link HeaderRegistry} IDs.
 *
 * <p>Provides fast, allocation-free resolution of header names to integer IDs
 * using length-based dispatch and case-insensitive comparison. This allows
 * O(1) array-based lookups in {@link HttpHeaderIndex}.</p>
 *
 * <p>Supports both Slice (zero-copy) and String-based resolution.</p>
 *
 * @see HeaderRegistry
 * @see HttpHeaderIndex
 */
public final class HeaderResolver {

    /**
     * Resolves a header name Slice to its HeaderRegistry ID.
     *
     * @param name the header name as a Slice
     * @return the header ID, or -1 if not a known header
     */
    public static int resolve(Slice name) {
        int len = name.length();

        switch (len) {

            case 4: // Host
                if (eq(name, "host")) return HeaderRegistry.HOST;
                break;

            case 6: // Accept
                if (eq(name, "accept")) return HeaderRegistry.ACCEPT;
                break;

            case 7: // Cookie
                if (eq(name, "cookie")) return HeaderRegistry.COOKIE;
                break;

            case 10: // Connection, User-Agent
                if (eq(name, "connection")) return HeaderRegistry.CONNECTION;
                if (eq(name, "user-agent")) return HeaderRegistry.USER_AGENT;
                break;

            case 12: // Content-Type
                if (eq(name, "content-type")) return HeaderRegistry.CONTENT_TYPE;
                break;

            case 14: // Content-Length
                if (eq(name, "content-length")) return HeaderRegistry.CONTENT_LENGTH;
                break;

            case 13: // Authorization
                if (eq(name, "authorization")) return HeaderRegistry.AUTHORIZATION;
                break;
        }

        return -1;
    }

    private static boolean eq(Slice slice, String s) {
        if (slice.length() != s.length()) return false;

        for (int i = 0; i < s.length(); i++) {
            byte a = slice.byteAt(i);
            char b = s.charAt(i);

            if (toLowerAscii(a) != toLowerAscii((byte) b)) return false;
        }
        return true;
    }

    private static byte toLowerAscii(byte b) {
        return (b >= 'A' && b <= 'Z') ? (byte)(b + 32) : b;
    }

    public static int resolveString(String name) {
        int len = name.length();

        switch (len) {
            case 4:  return name.equalsIgnoreCase("host") ? HeaderRegistry.HOST : -1;
            case 6:  return name.equalsIgnoreCase("accept") ? HeaderRegistry.ACCEPT : (name.equalsIgnoreCase("cookie") ? HeaderRegistry.COOKIE : -1);
            case 10:
                if (name.equalsIgnoreCase("connection")) return HeaderRegistry.CONNECTION;
                if (name.equalsIgnoreCase("user-agent")) return HeaderRegistry.USER_AGENT;
                break;
            case 12: return name.equalsIgnoreCase("content-type") ? HeaderRegistry.CONTENT_TYPE : -1;
            case 14: return name.equalsIgnoreCase("content-length") ? HeaderRegistry.CONTENT_LENGTH : -1;
            case 13: return name.equalsIgnoreCase("authorization") ? HeaderRegistry.AUTHORIZATION : -1;
        }

        return -1;
    }
}