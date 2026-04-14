package net.magnesiumbackend.core.annotations;

import net.magnesiumbackend.core.http.websocket.WebSocketHandler;
import net.magnesiumbackend.core.http.websocket.WebSocketSession;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method to handle WebSocket connection open events.
 *
 * <p>Methods annotated with @OnOpen are invoked when a new WebSocket
 * connection is established. The method must accept a {@link WebSocketSession}
 * parameter.</p>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * @RestController
 * public class ChatController {
 *     @OnOpen(path = "/chat/{roomId}")
 *     void onConnect(WebSocketSession session) {
 *         String roomId = session.pathVariables().get("roomId");
 *         // Handle new connection
 *     }
 * }
 * }</pre>
 *
 * @see WebSocketHandler#onOpen(WebSocketSession)
 * @see OnWebSocketMessage
 * @see OnWebSocketClose
 * @see OnWebSocketException
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface OnWebSocketOpen {
    /**
     * The WebSocket path this handler applies to.
     * Defaults to "/" if not specified.
     *
     * @return the WebSocket path
     */
    String path() default "";
}
