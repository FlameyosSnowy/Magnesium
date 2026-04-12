package net.magnesiumbackend.transport.undertow.websocket;

import io.github.flameyossnowy.velocis.tables.ConcurrentHashTable;
import io.github.flameyossnowy.velocis.tables.Table;
import net.magnesiumbackend.core.http.websocket.WebSocketSession;
import net.magnesiumbackend.core.http.websocket.WebSocketSessionManager;
import net.magnesiumbackend.core.json.JsonProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;

public class UndertowWebSocketSessionManager implements WebSocketSessionManager {
    private final Table<String, String, WebSocketSession> sessionsByPath =
        new ConcurrentHashTable<>();

    @Override
    public void add(String path, WebSocketSession session) {
        sessionsByPath.put(path, session.id(), session);
    }

    @Override
    public void remove(String path, @NotNull WebSocketSession session) {
        sessionsByPath.remove(path, session.id());
    }

    @Override
    public Collection<WebSocketSession> all() {
        return sessionsByPath.values();
    }

    @Override
    public void broadcast(String path, Object message, JsonProvider provider) {
        Map<String, WebSocketSession> sessions = sessionsByPath.row(path);
        if (sessions.isEmpty()) return;
        String text = message instanceof String stringMessage ? stringMessage : provider.toJson(message);
        for (WebSocketSession s : sessions.values()) {
            s.sendText(text);
        }
    }
}