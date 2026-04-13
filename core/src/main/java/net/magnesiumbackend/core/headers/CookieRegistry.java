package net.magnesiumbackend.core.headers;

/**
 * Registry of well-known cookie names for fast lookup via array indexing.
 *
 * <p>Known cookies (session, csrf, userId) are assigned integer IDs for O(1)
 * array-based lookups in {@link CookieIndex}. This avoids HashMap overhead
 * for the most common cookie names.</p>
 *
 * <h3>Known Cookies</h3>
 * <ul>
 *   <li>{@link #SESSION} - Session identifier</li>
 *   <li>{@link #CSRF} - CSRF token</li>
 *   <li>{@link #USER_ID} - User identifier</li>
 * </ul>
 *
 * @see CookieIndex
 * @see HeaderRegistry
 */
public final class CookieRegistry {

    /** Session cookie ID. */
    public static final int SESSION = 0;

    /** CSRF token cookie ID. */
    public static final int CSRF = 1;

    /** User ID cookie ID. */
    public static final int USER_ID = 2;

    /** Total number of known cookie slots. */
    public static final int COUNT = 8;

    public static int resolve(String name) {
        if (name == null) return -1;

        return switch (name.length()) {
            case 7 -> name.equalsIgnoreCase("session") ? SESSION : -1;
            case 4 -> name.equalsIgnoreCase("csrf") ? CSRF : -1;
            case 6 -> name.equalsIgnoreCase("userId") ? USER_ID : -1;
            default -> -1;
        };
    }

    public static int resolve(Slice slice) {
        if (slice == null) return -1;

        int len = slice.length();

        return switch (len) {
            case 7 -> slice.equalsIgnoreCase("session") ? SESSION : -1;
            case 4 -> slice.equalsIgnoreCase("csrf") ? CSRF : -1;
            case 6 -> slice.equalsIgnoreCase("userId") ? USER_ID : -1;
            default -> -1;
        };
    }

    private CookieRegistry() {}
}