package net.magnesiumbackend.core.annotations;

import net.magnesiumbackend.core.http.websocket.WebSocketHandler;
import net.magnesiumbackend.core.http.websocket.WebSocketSession;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method to handle WebSocket connection close events.
 *
 * <p>Methods annotated with @OnClose are invoked when a WebSocket
 * connection is closed. The method should accept the session, status code,
 * and close reason as parameters.</p>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * @RestController
 * public class ChatController {
 *     @OnClose(path = "/chat/{roomId}")
 *     void onDisconnect(WebSocketSession session, int code, String reason) {
 *         String roomId = session.pathVariables().get("roomId");
 *         // Handle connection close
 *     }
 * }
 * }</pre>
 *
 * @see WebSocketHandler#onClose(WebSocketSession, int, String)
 * @see OnOpen
 * @see OnMessage
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface OnClose {
    /**
     * The WebSocket path this handler applies to.
     * Defaults to "/" if not specified.
     *
     * @return the WebSocket path
     */
    String path() default "";
}
