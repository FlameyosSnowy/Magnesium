package net.magnesiumbackend.core.http.socket;

import net.magnesiumbackend.core.json.JsonProvider;

import java.util.Collection;
import java.util.Map;

public interface WebSocketSessionManager {
    Collection<WebSocketSession> all();

    void add(String path, WebSocketSession session);

    void remove(String path, WebSocketSession session);

    void broadcast(String path, Object message, JsonProvider jsonProvider);
}