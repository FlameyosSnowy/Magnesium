package net.magnesiumbackend.core.http.websocket;

import net.magnesiumbackend.core.json.JsonProvider;

import java.util.Collection;

/**
 * Manages WebSocket sessions for broadcasting and session tracking.
 *
 * <p>WebSocketSessionManager provides access to all active WebSocket sessions
 * organized by their registered path. It supports broadcasting messages to
 * all sessions on a specific path.</p>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * // Inject or obtain the session manager
 * WebSocketSessionManager manager = ...;
 *
 * // Broadcast to all connected clients on /chat/room1
 * manager.broadcast("/chat/room1", message, jsonProvider);
 *
 * // Get all active sessions
 * for (WebSocketSession session : manager.all()) {
 *     if (session.isOpen()) {
 *         session.sendText("Server notification");
 *     }
 * }
 * }</pre>
 *
 * @see WebSocketSession
 * @see WebSocketRouteRegistry
 */
public interface WebSocketSessionManager {

    /**
     * Returns all active WebSocket sessions.
     *
     * @return collection of all sessions
     */
    Collection<WebSocketSession> all();

    /**
     * Adds a session for the given path.
     *
     * @param path    the WebSocket endpoint path
     * @param session the session to add
     */
    void add(String path, WebSocketSession session);

    /**
     * Removes a session from the given path.
     *
     * @param path    the WebSocket endpoint path
     * @param session the session to remove
     */
    void remove(String path, WebSocketSession session);

    /**
     * Broadcasts a message to all sessions on the given path.
     *
     * @param path         the WebSocket endpoint path
     * @param message      the message object to broadcast
     * @param jsonProvider the JSON provider for serialization
     */
    void broadcast(String path, Object message, JsonProvider jsonProvider);
}