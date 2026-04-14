package net.magnesiumbackend.core.headers;

/**
 * Registry of well-known HTTP header names for fast lookup via array indexing.
 *
 * <p>Common headers (Host, Content-Type, Authorization, etc.) are assigned
 * integer IDs for O(1) array-based lookups in {@link HttpHeaderIndex}. This
 * avoids HashMap overhead for the most frequently accessed headers.</p>
 *
 * <h3>Known Headers</h3>
 * <ul>
 *   <li>{@link #HOST} - Host header</li>
 *   <li>{@link #CONTENT_TYPE} - Content-Type header</li>
 *   <li>{@link #CONTENT_LENGTH} - Content-Length header</li>
 *   <li>{@link #AUTHORIZATION} - Authorization header</li>
 *   <li>{@link #COOKIE} - Cookie header</li>
 *   <li>{@link #USER_AGENT} - User-Agent header</li>
 *   <li>{@link #ACCEPT} - Accept header</li>
 *   <li>{@link #CONNECTION} - Connection header</li>
 * </ul>
 *
 * @see HttpHeaderIndex
 * @see HeaderResolver
 * @see CookieRegistry
 */
public final class HeaderRegistry {

    /** Host header ID. */
    public static final int HOST            = 0;

    /** Content-Type header ID. */
    public static final int CONTENT_TYPE    = 1;

    /** Content-Length header ID. */
    public static final int CONTENT_LENGTH  = 2;

    /** Authorization header ID. */
    public static final int AUTHORIZATION   = 3;

    /** Cookie header ID. */
    public static final int COOKIE          = 4;

    /** User-Agent header ID. */
    public static final int USER_AGENT      = 5;

    /** Accept header ID. */
    public static final int ACCEPT          = 6;

    /** Connection header ID. */
    public static final int CONNECTION      = 7;

    /** Total number of known header slots. */
    public static final int COUNT = 8;

    private static final String[] NAMES = new String[COUNT];

    static {
        NAMES[HOST] = "host";
        NAMES[CONTENT_TYPE] = "content-type";
        NAMES[CONTENT_LENGTH] = "content-length";
        NAMES[AUTHORIZATION] = "authorization";
        NAMES[COOKIE] = "cookie";
        NAMES[USER_AGENT] = "user-agent";
        NAMES[ACCEPT] = "accept";
        NAMES[CONNECTION] = "connection";
    }

    private HeaderRegistry() {}

    /**
     * @return canonical lowercase header name for index.
     */
    public static String nameOf(int index) {
        if (index < 0 || index >= COUNT) {
            throw new IndexOutOfBoundsException("Invalid header id: " + index);
        }
        return NAMES[index];
    }
}