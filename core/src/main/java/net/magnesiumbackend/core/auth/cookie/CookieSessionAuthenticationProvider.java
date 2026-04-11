package net.magnesiumbackend.core.auth.cookie;

import net.magnesiumbackend.core.auth.*;
import net.magnesiumbackend.core.auth.exceptions.AuthenticationException;
import net.magnesiumbackend.core.route.RequestContext;
import java.util.Optional;

public final class CookieSessionAuthenticationProvider implements AuthenticationProvider {

    private final SessionStore sessionStore;
    private final String cookieName;

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