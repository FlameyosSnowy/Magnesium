package net.magnesiumbackend.core.auth.cookie;

import java.util.Optional;
import java.util.Set;

public interface SessionStore {
    Optional<Session> lookup(String sessionId);

    record Session(String userId, String username, Set<String> permissions) {}
}