package net.magnesiumbackend.core.auth.cookie;

import net.magnesiumbackend.core.auth.*;
import net.magnesiumbackend.core.auth.exceptions.AuthenticationException;
import net.magnesiumbackend.core.route.RequestContext;
import java.util.Optional;

/**
 * Session cookie authentication provider.
 *
 * <p>Extracts session IDs from cookies and looks up session data in a
 * {@link SessionStore}. Useful for traditional browser-based session authentication.</p>
 *
 * <h3>Authentication Flow</h3>
 * <ol>
 *   <li>Extracts session ID from the configured cookie name</li>
 *   <li>Looks up session in the {@link SessionStore}</li>
 *   <li>Returns Principal if session exists, empty if no cookie or expired</li>
 * </ol>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * SessionStore store = new RedisSessionStore(redis); // or InMemorySessionStore
 * CookieSessionAuthenticationProvider provider =
 *     new CookieSessionAuthenticationProvider(store, "session_id");
 * }</pre>
 *
 * @see SessionStore
 * @see net.magnesiumbackend.core.auth.AuthenticationProvider
 */
public final class CookieSessionAuthenticationProvider implements AuthenticationProvider {

    private final SessionStore sessionStore;
    private final String cookieName;

    /**
     * Creates a new cookie session provider.
     *
     * @param sessionStore the backing store for session lookups
     * @param cookieName   the name of the session cookie (e.g., "session_id")
     */
    public CookieSessionAuthenticationProvider(SessionStore sessionStore, String cookieName) {
        this.sessionStore = sessionStore;
        this.cookieName   = cookieName;
    }

    @Override
    public Optional<Principal> authenticate(RequestContext ctx) throws AuthenticationException {
        String sessionId = ctx.cookie(cookieName);
        if (sessionId == null) {
            return Optional.empty(); // no session cookie
        }

        return sessionStore.lookup(sessionId)
            .map(session -> Principal.of(
                session.userId(),
                session.username(),
                session.permissions()
            ));
        // expired/invalid session → store returns empty, we return empty → 401 downstream
    }
}