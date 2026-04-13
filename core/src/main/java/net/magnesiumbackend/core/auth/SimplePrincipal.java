package net.magnesiumbackend.core.auth;

import java.util.Objects;
import java.util.Set;

/**
 * Immutable implementation of {@link Principal} for authenticated and anonymous users.
 *
 * <p>SimplePrincipal is the standard implementation used throughout Magnesium for
 * representing both authenticated users (with identity and permissions) and anonymous
 * users (no identity, no permissions).</p>
 *
 * <p>Use the factory methods to create instances:</p>
 * <ul>
 *   <li>{@link Principal#of(String, String, Set)} - for authenticated users</li>
 *   <li>{@link Principal#anonymous()} - for anonymous/unauthenticated users</li>
 * </ul>
 *
 * @see Principal
 * @see Principal#of(String, String, Set)
 * @see Principal#anonymous()
 */
public final class SimplePrincipal implements Principal {
    private final String userId;
    private final String username;
    private final Set<String> permissions;
    private final boolean anonymous;

    /**
     * Creates a principal for an authenticated user.
     *
     * @param userId      the unique user identifier
     * @param username    the human-readable username
     * @param permissions the set of granted permissions
     */
    SimplePrincipal(String userId, String username, Set<String> permissions) {
        this(userId, username, permissions, false);
    }

    /**
     * Creates a principal, optionally anonymous.
     *
     * @param userId      the unique user identifier (empty for anonymous)
     * @param username    the human-readable username (empty for anonymous)
     * @param permissions the set of granted permissions (empty for anonymous)
     * @param anonymous   true if this is an anonymous principal
     */
    SimplePrincipal(String userId, String username, Set<String> permissions, boolean anonymous) {
        this.userId = userId;
        this.username = username;
        this.permissions = permissions;
        this.anonymous = anonymous;
    }

    @Override
    public String userId() {
        return userId;
    }

    @Override
    public String username() {
        return username;
    }

    @Override
    public Set<String> permissions() {
        return permissions;
    }

    @Override
    public boolean isAnonymous() {
        return anonymous;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != getClass()) return false;
        var that = (SimplePrincipal) obj;
        return Objects.equals(this.userId, that.userId) &&
            Objects.equals(this.username, that.username) &&
            Objects.equals(this.permissions, that.permissions) &&
            this.anonymous == that.anonymous;
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(userId);
        result = 31 * result + Objects.hashCode(username);
        result = 31 * result + Objects.hashCode(permissions);
        result = 31 * result + Boolean.hashCode(anonymous);
        return result;
    }

    @Override
    public String toString() {
        return "SimplePrincipal[" +
            "userId=" + userId + ", " +
            "username=" + username + ", " +
            "permissions=" + permissions + ", " +
            "anonymous=" + anonymous + ']';
    }

}