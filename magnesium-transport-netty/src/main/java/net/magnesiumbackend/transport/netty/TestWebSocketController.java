package net.magnesiumbackend.transport.netty;

import net.magnesiumbackend.core.annotations.RestController;
import net.magnesiumbackend.core.annotations.WebSocketMapping;
import net.magnesiumbackend.core.http.socket.WebSocketMessage;
import net.magnesiumbackend.core.http.socket.WebSocketSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class TestWebSocketController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestWebSocketController.class);

    @WebSocketMapping(path = "/ws/test")
    public void onOpen(WebSocketSession session) {
        LOGGER.info("[WS] Client connected: {}", session.id());
        session.sendText("Welcome! Your session id is: " + session.id());
    }

    @WebSocketMapping(path = "/ws/test")
    public void onMessage(WebSocketSession session, WebSocketMessage message) {
        if (message.isText()) {
            String text = message.asText();
            LOGGER.info("[WS] Text from {}: {}", session.id(), text);
            session.sendText("Echo: " + text);
        } else if (message.isBinary()) {
            byte[] data = message.asBinary();
            LOGGER.info("[WS] Binary from {}: {} bytes", session.id(), data.length);
            session.sendBinary(data);
        }
    }

    @WebSocketMapping(path = "/ws/test")
    public void onClose(WebSocketSession session, int statusCode, String reason) {
        LOGGER.info("[WS] Client disconnected: {}, {} {}", session.id(), statusCode, reason);
    }

    @WebSocketMapping(path = "/ws/test")
    public void onError(WebSocketSession session, Throwable error) {
        LOGGER.error("[WS] Error from {}: {}", session.id(), error.getMessage(), error);
    }
}