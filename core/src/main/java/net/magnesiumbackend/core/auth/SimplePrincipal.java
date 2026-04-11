package net.magnesiumbackend.core.auth;

import java.util.Objects;
import java.util.Set;

public final class SimplePrincipal implements Principal {
    private final String userId;
    private final String username;
    private final Set<String> permissions;
    private final boolean anonymous;

    SimplePrincipal(String userId, String username, Set<String> permissions) {
        this(userId, username, permissions, false);
    }

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