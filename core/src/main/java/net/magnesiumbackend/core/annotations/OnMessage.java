package net.magnesiumbackend.core.annotations;

import net.magnesiumbackend.core.http.websocket.WebSocketHandler;
import net.magnesiumbackend.core.http.websocket.WebSocketMessage;
import net.magnesiumbackend.core.http.websocket.WebSocketSession;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method to handle incoming WebSocket messages.
 *
 * <p>Methods annotated with @OnMessage are invoked when a message is received
 * from a client. The method must accept {@link WebSocketSession} and
 * {@link WebSocketMessage} parameters.</p>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * @RestController
 * public class ChatController {
 *     @OnMessage(path = "/chat/{roomId}")
 *     void onMessage(WebSocketSession session, WebSocketMessage message) {
 *         String text = message.asText();
 *         session.sendText("Echo: " + text);
 *     }
 * }
 * }</pre>
 *
 * @see WebSocketHandler#onMessage(WebSocketSession, WebSocketMessage)
 * @see OnOpen
 * @see OnClose
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface OnMessage {
    /**
     * The WebSocket path this handler applies to.
     * Defaults to "/" if not specified.
     *
     * @return the WebSocket path
     */
    String path() default "";
}
