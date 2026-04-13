package net.magnesiumbackend.core.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Maps WebSocket connections to a handler method.
 *
 * <p>The annotated method will be invoked when a WebSocket upgrade request
 * matches the specified path pattern. WebSocket handlers manage bidirectional
 * communication between client and server.</p>
 *
 * <p>The annotated method should return a {@link net.magnesiumbackend.core.http.websocket.WebSocketHandler}
 * or a compatible handler interface that manages connection lifecycle events:</p>
 * <ul>
 *   <li>{@code onOpen} - Called when a connection is established</li>
 *   <li>{@code onMessage} - Called when a message is received</li>
 *   <li>{@code onClose} - Called when a connection is closed</li>
 *   <li>{@code onError} - Called when an error occurs</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * @RestController
 * public class ChatController {
 *     @WebSocketMapping(path = "/chat/{roomId}")
 *     public WebSocketHandler handleChat(@PathParam String roomId) {
 *         return new WebSocketHandler() {
 *             @Override
 *             public void onOpen(WebSocketSession session) {
 *                 chatService.join(roomId, session);
 *             }
 *
 *             @Override
 *             public void onMessage(WebSocketSession session, String message) {
 *                 chatService.broadcast(roomId, message);
 *             }
 *
 *             @Override
 *             public void onClose(WebSocketSession session, int code, String reason) {
 *                 chatService.leave(roomId, session);
 *             }
 *         };
 *     }
 * }
 * }</pre>
 *
 * @see RestController
 * @see PathParam
 * @see net.magnesiumbackend.core.http.websocket.WebSocketHandler
 * @see net.magnesiumbackend.core.http.websocket.WebSocketSession
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface WebSocketMapping {
    /**
     * The path pattern for this WebSocket endpoint.
     *
     * <p>May include path variables in curly braces, e.g., {@code "/chat/{roomId}"}.
     * Exact paths are also valid.</p>
     *
     * @return the route path pattern
     */
    String path();
}
