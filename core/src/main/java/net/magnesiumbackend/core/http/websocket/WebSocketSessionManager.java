package net.magnesiumbackend.core.http.websocket;

import net.magnesiumbackend.core.json.JsonProvider;

import java.util.Collection;

public interface WebSocketSessionManager {
    Collection<WebSocketSession> all();

    void add(String path, WebSocketSession session);

    void remove(String path, WebSocketSession session);

    void broadcast(String path, Object message, JsonProvider jsonProvider);
}