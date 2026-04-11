package net.magnesiumbackend.transport.httpserver;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import net.magnesiumbackend.core.http.socket.DefaultWebSocketMessage;
import net.magnesiumbackend.core.http.socket.WebSocketHandler;
import net.magnesiumbackend.core.http.socket.WebSocketSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import robaho.net.httpserver.websockets.CloseCode;
import robaho.net.httpserver.websockets.OpCode;
import robaho.net.httpserver.websockets.WebSocket;
import robaho.net.httpserver.websockets.WebSocketException;
import robaho.net.httpserver.websockets.WebSocketFrame;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MagnesiumWebSocket extends WebSocket {
    private static final Logger LOGGER = LoggerFactory.getLogger(MagnesiumWebSocket.class);

    private final WebSocketHandler handler;
    private final WebSocketSessionManager sessionManager;
    private final String path;
    private final HttpServerWebSocketSession session;

    private StringBuilder textBuffer   = new StringBuilder(32);
    private byte[]        binaryBuffer = new byte[0];

    public MagnesiumWebSocket(
        HttpExchange exchange,
        WebSocketHandler handler,
        WebSocketSessionManager sessionManager,
        String path,
        Map<String, String> pathVariables
    ) {
        super(exchange);
        this.handler        = handler;
        this.sessionManager = sessionManager;
        this.path           = path;

        Headers requestHeaders = exchange.getRequestHeaders();
        Map<String, String> headers = new HashMap<>(requestHeaders.size());
        for (Map.Entry<String, List<String>> entry : requestHeaders.entrySet()) {
            String name = entry.getKey();
            List<String> values = entry.getValue();
            if (!values.isEmpty()) headers.put(name, values.getFirst());
        }

        this.session = new HttpServerWebSocketSession(this, pathVariables, headers);
    }

    @Override
    public void onOpen() {
        sessionManager.add(path, session);
        try {
            handler.onOpen(session);
        } catch (Exception e) {
            LOGGER.error("WebSocket onOpen error", e);
        }
    }

    @Override
    protected void onMessage(WebSocketFrame frame) {
        try {
            if (frame.getOpCode() == OpCode.Binary) {
                byte[] chunk = frame.getBinaryPayload();
                binaryBuffer = concat(binaryBuffer, chunk);
                if (frame.isFin()) {
                    handler.onMessage(session, DefaultWebSocketMessage.ofBinary(binaryBuffer));
                    binaryBuffer = new byte[0];
                }
            } else {
                textBuffer.append(frame.getTextPayload());
                if (frame.isFin()) {
                    handler.onMessage(session, DefaultWebSocketMessage.ofText(textBuffer.toString()));
                    textBuffer = new StringBuilder();
                }
            }
        } catch (Exception e) {
            LOGGER.error("WebSocket onMessage error", e);
        }
    }

    @Override
    protected void onClose(CloseCode code, String reason, boolean initiatedByRemote) {
        sessionManager.remove(path, session);
        try {
            handler.onClose(session, code.getValue(), reason);
        } catch (Exception e) {
            LOGGER.error("WebSocket onClose error", e);
        }
    }

    @Override
    protected void onPong(WebSocketFrame pong) {
    }

    @Override
    protected void onException(IOException e) {
        LOGGER.error("WebSocket channel error", e);
        try {
            handler.onError(session, e);
        } catch (Exception ex) {
            LOGGER.error("WebSocket onError handler threw", ex);
        }
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }
}