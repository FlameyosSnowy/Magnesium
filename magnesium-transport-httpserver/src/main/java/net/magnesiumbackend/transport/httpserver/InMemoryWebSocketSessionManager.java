package net.magnesiumbackend.transport.httpserver;

import net.magnesiumbackend.core.http.websocket.WebSocketSession;
import net.magnesiumbackend.core.http.websocket.WebSocketSessionManager;
import net.magnesiumbackend.core.json.JsonProvider;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * In-memory implementation of WebSocketSessionManager for the JDK HttpServer transport.
 */
public class InMemoryWebSocketSessionManager implements WebSocketSessionManager {

    private final Map<String, Set<WebSocketSession>> sessionsByPath = new ConcurrentHashMap<>();

    @Override
    public Collection<WebSocketSession> all() {
        Set<WebSocketSession> all = ConcurrentHashMap.newKeySet();
        sessionsByPath.values().forEach(all::addAll);
        return all;
    }

    @Override
    public void add(String path, WebSocketSession session) {
        sessionsByPath.computeIfAbsent(path, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    @Override
    public void remove(String path, WebSocketSession session) {
        Set<WebSocketSession> sessions = sessionsByPath.get(path);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                sessionsByPath.remove(path);
            }
        }
    }

    @Override
    public void broadcast(String path, Object message, JsonProvider jsonProvider) {
        Set<WebSocketSession> sessions = sessionsByPath.get(path);
        if (sessions != null) {
            String json = jsonProvider.toJson(message);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendText(json);
                }
            }
        }
    }
}
