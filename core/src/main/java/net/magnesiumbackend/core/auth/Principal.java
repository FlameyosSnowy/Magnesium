package net.magnesiumbackend.core.auth;

import java.util.Set;

public interface Principal {
    String userId();
    String username();
    Set<String> permissions();

    default boolean hasPermission(String permission) {
        return permissions().contains(permission);
    }

    default void mustOwn(String resourceOwnerId, Runnable ifError) {
        if (!userId().equals(resourceOwnerId)) {
            ifError.run();
        }
    }

    static Principal anonymous() {
        return new SimplePrincipal("", "", Set.of(), true);
    }

    static Principal of(String userId, String username, Set<String> permissions) {
        return new SimplePrincipal(userId, username, permissions);
    }

    boolean isAnonymous();
}