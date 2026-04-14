package net.magnesiumbackend.core.annotations;

import net.magnesiumbackend.core.http.websocket.WebSocketHandler;
import net.magnesiumbackend.core.http.websocket.WebSocketSession;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method to handle WebSocket errors.
 *
 * <p>Methods annotated with @OnException are invoked when an error occurs
 * on a WebSocket connection. The method should accept the session and the
 * throwable that caused the error.</p>
 *
 * <h3>Usage Example</h3>
 * <pre>{@code
 * @RestController
 * public class ChatController {
 *     @OnException(path = "/chat/{roomId}")
 *     void onError(WebSocketSession session, Throwable error) {
 *         // Log error, close connection, etc.
 *     }
 * }
 * }</pre>
 *
 * @see WebSocketHandler#onError(WebSocketSession, Throwable)
 * @see OnOpen
 * @see OnMessage
 * @see OnClose
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface OnException {
    /**
     * The WebSocket path this handler applies to.
     * Defaults to "/" if not specified.
     *
     * @return the WebSocket path
     */
    String path() default "";
}
