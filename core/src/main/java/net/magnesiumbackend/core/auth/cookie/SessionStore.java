package net.magnesiumbackend.core.auth.cookie;

import java.util.Optional;
import java.util.Set;

/**
 * Storage interface for session-based authentication.
 *
 * <p>SessionStore implementations manage the lifecycle of server-side sessions,
 * storing session data (user identity, permissions) indexed by session ID.</p>
 *
 * <h3>Implementations</h3>
 * <ul>
 *   <li>In-memory (development/testing)</li>
 *   <li>Redis (distributed, production)</li>
 *   <li>Database (persistent sessions)</li>
 * </ul>
 *
 * <p>Example custom implementation with Redis:</p>
 * <pre>{@code
 * public class RedisSessionStore implements SessionStore {
 *     private final JedisPool jedisPool;
 *
 *     public Optional<Session> lookup(String sessionId) {
 *         try (Jedis jedis = jedisPool.getResource()) {
 *             String json = jedis.get("session:" + sessionId);
 *             return json != null ? Optional.of(parse(json)) : Optional.empty();
 *         }
 *     }
 * }
 * }</pre>
 *
 * @see CookieSessionAuthenticationProvider
 */
public interface SessionStore {
    /**
     * Looks up a session by its ID.
     *
     * <p>Returns empty if the session doesn't exist or has expired.</p>
     *
     * @param sessionId the session identifier from the cookie
     * @return the session data if found, otherwise empty
     */
    Optional<Session> lookup(String sessionId);

    /**
     * Immutable record containing session data.
     *
     * @param userId      the unique user identifier
     * @param username    the human-readable username
     * @param permissions the set of granted permissions
     */
    record Session(String userId, String username, Set<String> permissions) {}
}