package net.magnesiumbackend.transport.httpserver;

import net.magnesiumbackend.core.MagnesiumApplication;
import net.magnesiumbackend.core.MagnesiumRuntime;
import net.magnesiumbackend.core.headers.HttpHeaderIndex;
import net.magnesiumbackend.core.headers.HttpPathParamIndex;
import net.magnesiumbackend.core.http.MagnesiumHttpServer;
import net.magnesiumbackend.core.http.response.ResponseEntity;
import net.magnesiumbackend.core.http.websocket.WebSocketHandler;
import net.magnesiumbackend.core.http.websocket.WebSocketMessage;
import net.magnesiumbackend.core.http.websocket.WebSocketSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for WebSocket functionality in JDK HttpServer transport.
 */
class HttpServerWebSocketTest {

    private static final String WEBSOCKET_MAGIC_STRING = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    private HttpServerMagnesiumTransport transport;
    private MagnesiumRuntime application;
    private int actualPort;

    @BeforeEach
    void setUp() {
        // Each test sets up its own server
    }

    @AfterEach
    void tearDown() {
        if (transport != null) {
            transport.shutdown();
        }
    }

    /**
     * Creates and starts a test application with the given WebSocket handler.
     */
    private void startWebSocketServer(String path, WebSocketHandler handler) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        MagnesiumHttpServer httpServer = MagnesiumHttpServer.builder()
            .websocket(path, handler)
            .get("/health", ctx -> ResponseEntity.ok(Map.of("status", "up")))
            .build();

        application = new MagnesiumRuntime(null);
        application.router()
            .websocket(path, handler)
            .get("/health", ctx -> ResponseEntity.ok(Map.of("status", "up")))
                .commit();

        transport = new HttpServerMagnesiumTransport();

        Thread serverThread = new Thread(() -> {
            try {
                transport.bind(0, application, application.router().routes());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Server failed to start");

        // Small delay to ensure server is fully ready
        Thread.sleep(100);

        actualPort = transport.getPort();
        assertTrue(actualPort > 0, "Server should be running on a valid port");
    }

    @Test
    void testWebSocketHandshake() throws Exception {
        AtomicReference<WebSocketSession> sessionRef = new AtomicReference<>();

        startWebSocketServer("/ws/test", new WebSocketHandler() {
            @Override
            public void onOpen(WebSocketSession session) {
                sessionRef.set(session);
                
            }
        });

        // Perform WebSocket handshake
        Socket socket = new Socket("localhost", actualPort);

        // Send WebSocket upgrade request
        OutputStream out = socket.getOutputStream();
        String key = generateWebSocketKey();
        String request = String.format(
            "GET /ws/test HTTP/1.1\r\n" +
            "Host: localhost:%d\r\n" +
            "Upgrade: websocket\r\n" +
            "Connection: Upgrade\r\n" +
            "Sec-WebSocket-Key: %s\r\n" +
            "Sec-WebSocket-Version: 13\r\n" +
            "\r\n",
            actualPort, key
        );
        out.write(request.getBytes(StandardCharsets.UTF_8));
        out.flush();

        // Read response
        InputStream in = socket.getInputStream();
        byte[] buffer = new byte[1024];
        int read = in.read(buffer);
        String response = new String(buffer, 0, read, StandardCharsets.UTF_8);
        System.out.println("[DEBUG] WebSocket response: " + response.replace("\n", "\\n"));

        // Verify response is 101 Switching Protocols
        assertTrue(response.contains("101"), "Response should indicate 101 Switching Protocols");
        assertTrue(response.contains("Upgrade: websocket"), "Response should confirm WebSocket upgrade");

        // Verify Sec-WebSocket-Accept header is present (case-insensitive)
        assertTrue(response.toLowerCase().contains("sec-websocket-accept"), "Response should contain Sec-WebSocket-Accept header");

        closeWebSocketGracefully(socket);
    }

    @Test
    void testWebSocketConnectionWithPathVariables() throws Exception {
        AtomicReference<String> capturedRoomId = new AtomicReference<>();
        AtomicReference<HttpPathParamIndex> capturedPathVars = new AtomicReference<>();
        CountDownLatch openLatch = new CountDownLatch(1);

        startWebSocketServer("/ws/rooms/{roomId}", new WebSocketHandler() {
            @Override
            public void onOpen(WebSocketSession session) {
                capturedPathVars.set(session.pathVariables());
                capturedRoomId.set(session.pathVariables().get("roomId"));
                openLatch.countDown();
                
            }
        });

        // Connect with a specific room ID
        String roomId = "room-123";
        Socket socket = performWebSocketHandshake("/ws/rooms/" + roomId);

        // Wait for the onOpen handler to be called
        assertTrue(openLatch.await(2, TimeUnit.SECONDS), "WebSocket onOpen should have been called");

        assertEquals(roomId, capturedRoomId.get(), "Room ID should be captured from path variable");
        assertNotNull(capturedPathVars.get(), "Path variables should be available");
        assertNotNull(capturedPathVars.get().get("roomId"), "Path variables should contain roomId");

        closeWebSocketGracefully(socket);
    }

    @Test
    void testWebSocketTextMessageExchange() throws Exception {
        CountDownLatch messageLatch = new CountDownLatch(1);
        AtomicReference<String> receivedMessage = new AtomicReference<>();
        AtomicReference<WebSocketSession> sessionRef = new AtomicReference<>();

        startWebSocketServer("/ws/echo", new WebSocketHandler() {
            @Override
            public void onOpen(WebSocketSession session) {
                sessionRef.set(session);
                
            }

            @Override
            public void onMessage(WebSocketSession session, WebSocketMessage message) {
                if (message.isText()) {
                    receivedMessage.set(message.asText());
                    session.sendText("Echo: " + message.asText());
                    messageLatch.countDown();
                }
                
            }
        });

        Socket socket = performWebSocketHandshake("/ws/echo");

        // Send a text frame
        String testMessage = "Hello WebSocket!";
        sendWebSocketTextFrame(socket, testMessage);

        // Wait for and receive response
        assertTrue(messageLatch.await(2, TimeUnit.SECONDS), "Message should be received");
        assertEquals(testMessage, receivedMessage.get(), "Received message should match sent message");

        // Read the echo response
        String response = readWebSocketTextFrame(socket);
        assertTrue(response.contains("Echo: Hello WebSocket!"), "Should receive echo response");

        closeWebSocketGracefully(socket);
    }

    @Test
    void testMultipleWebSocketClients() throws Exception {
        AtomicInteger connectionCount = new AtomicInteger(0);
        CountDownLatch firstClientLatch = new CountDownLatch(1);
        CountDownLatch secondClientLatch = new CountDownLatch(1);

        startWebSocketServer("/ws/chat", new WebSocketHandler() {
            @Override
            public void onOpen(WebSocketSession session) {
                int count = connectionCount.incrementAndGet();
                if (count == 1) {
                    firstClientLatch.countDown();
                } else if (count == 2) {
                    secondClientLatch.countDown();
                }
                
            }
        });

        // Connect first client
        Socket client1 = performWebSocketHandshake("/ws/chat");
        assertTrue(firstClientLatch.await(2, TimeUnit.SECONDS), "First client should connect");

        // Connect second client
        Socket client2 = performWebSocketHandshake("/ws/chat");
        assertTrue(secondClientLatch.await(2, TimeUnit.SECONDS), "Second client should connect");

        assertEquals(2, connectionCount.get(), "Should have 2 connections");

        closeWebSocketGracefully(client1);
        closeWebSocketGracefully(client2);
    }

    @Test
    void testWebSocketSessionHeaders() throws Exception {
        AtomicReference<HttpHeaderIndex> capturedHeaders = new AtomicReference<>();
        CountDownLatch openLatch = new CountDownLatch(1);

        startWebSocketServer("/ws/headers", new WebSocketHandler() {
            @Override
            public void onOpen(WebSocketSession session) {
                capturedHeaders.set(session.headers());
                openLatch.countDown();
                
            }
        });

        Socket socket = performWebSocketHandshake("/ws/headers");

        assertTrue(openLatch.await(2, TimeUnit.SECONDS), "onOpen should be called");

        HttpHeaderIndex headers = capturedHeaders.get();
        assertNotNull(headers, "Headers should be captured");
        // The headers should contain the upgrade information
        assertTrue(headers.get("Upgrade") != null || headers.get("upgrade") != null,
            "Headers should contain Upgrade header");

        closeWebSocketGracefully(socket);
    }

    @Test
    void testNonWebSocketRequestToWebSocketPath() throws Exception {
        startWebSocketServer("/ws-only", new WebSocketHandler() {
            @Override
            public void onOpen(WebSocketSession session) {
                // Should not be called for non-WebSocket requests
                
            }
        });

        // Make a regular HTTP GET request (not WebSocket upgrade)
        Socket socket = new Socket("localhost", actualPort);
        OutputStream out = socket.getOutputStream();

        String request = String.format(
            """
            GET /ws-only HTTP/1.1\r
            Host: localhost:%d\r
            \r
            """,
            actualPort
        );
        out.write(request.getBytes(StandardCharsets.UTF_8));
        out.flush();

        InputStream in = socket.getInputStream();
        byte[] buffer = new byte[1024];
        int read = in.read(buffer);
        String response = new String(buffer, 0, read, StandardCharsets.UTF_8);

        // Should not be a 101 upgrade
        assertFalse(response.contains("101"), "Non-WebSocket request should not get 101 response");

        // Not a WebSocket connection, so regular close is fine
        socket.close();
    }

    @Test
    void testWebSocketWithQueryParameters() throws Exception {
        AtomicReference<String> capturedToken = new AtomicReference<>();
        CountDownLatch openLatch = new CountDownLatch(1);

        startWebSocketServer("/ws/auth", new WebSocketHandler() {
            @Override
            public void onOpen(WebSocketSession session) {
                // In a real implementation, query params would be available
                // For now, we just verify the connection succeeds
                openLatch.countDown();
            }
        });

        // Connect with query parameters
        Socket socket = new Socket("localhost", actualPort);
        OutputStream out = socket.getOutputStream();

        String key = generateWebSocketKey();
        String request = String.format(
            "GET /ws/auth?token=secret-token-123 HTTP/1.1\r\n" +
            "Host: localhost:%d\r\n" +
            "Upgrade: websocket\r\n" +
            "Connection: Upgrade\r\n" +
            "Sec-WebSocket-Key: %s\r\n" +
            "Sec-WebSocket-Version: 13\r\n" +
            "\r\n",
            actualPort, key
        );
        out.write(request.getBytes(StandardCharsets.UTF_8));
        out.flush();

        InputStream in = socket.getInputStream();
        byte[] buffer = new byte[1024];
        int read = in.read(buffer);
        String response = new String(buffer, 0, read, StandardCharsets.UTF_8);

        assertTrue(response.contains("101"), "WebSocket upgrade should succeed with query params");
        assertTrue(openLatch.await(2, TimeUnit.SECONDS), "onOpen should be called");

        closeWebSocketGracefully(socket);
    }

    @Test
    void testBidirectionalMessaging() throws Exception {
        CountDownLatch serverReceivedLatch = new CountDownLatch(1);
        CountDownLatch clientReceivedLatch = new CountDownLatch(1);
        AtomicReference<String> serverReceivedMessage = new AtomicReference<>();
        AtomicReference<String> clientReceivedMessage = new AtomicReference<>();
        AtomicReference<WebSocketSession> serverSessionRef = new AtomicReference<>();

        startWebSocketServer("/ws/bidirectional", new WebSocketHandler() {
            @Override
            public void onOpen(WebSocketSession session) {
                serverSessionRef.set(session);
                
            }

            @Override
            public void onMessage(WebSocketSession session, WebSocketMessage message) {
                if (message.isText()) {
                    serverReceivedMessage.set(message.asText());
                    serverReceivedLatch.countDown();
                    // Echo back with prefix to verify server-to-client works
                    session.sendText("Server received: " + message.asText());
                }
                
            }
        });

        Socket socket = performWebSocketHandshake("/ws/bidirectional");

        // Wait for connection to be established
        Thread.sleep(100);

        // Client sends message to server
        String clientMessage = "Hello from client!";
        sendWebSocketTextFrame(socket, clientMessage);

        // Verify server received the message
        assertTrue(serverReceivedLatch.await(2, TimeUnit.SECONDS), "Server should receive client message");
        assertEquals(clientMessage, serverReceivedMessage.get(), "Server received message should match");

        // Client reads server's response
        String serverResponse = readWebSocketTextFrame(socket);
        assertNotNull(serverResponse, "Client should receive server response");
        assertTrue(serverResponse.contains("Server received: " + clientMessage),
            "Server response should contain echoed message");

        // Verify bidirectional flow completed successfully
        System.out.println("[TEST] Bidirectional messaging verified: client->server: '" + clientMessage +
                          "', server->client: '" + serverResponse + "'");

        closeWebSocketGracefully(socket);
    }

    @Test
    void testWebSocketServerInitiatedMessage() throws Exception {
        CountDownLatch openLatch = new CountDownLatch(1);
        CountDownLatch messageReceivedLatch = new CountDownLatch(1);
        AtomicReference<String> receivedMessage = new AtomicReference<>();

        startWebSocketServer("/ws/push", new WebSocketHandler() {
            @Override
            public void onOpen(WebSocketSession session) {
                openLatch.countDown();
                // Server sends message immediately after connection
                session.sendText("Server push message");
                
            }

            @Override
            public void onMessage(WebSocketSession session, WebSocketMessage message) {
                if (message.isText()) {
                    receivedMessage.set(message.asText());
                    messageReceivedLatch.countDown();
                }
                
            }
        });

        Socket socket = performWebSocketHandshake("/ws/push");

        // Verify connection opened
        assertTrue(openLatch.await(2, TimeUnit.SECONDS), "Connection should open");

        // Wait for server-initiated message to be sent and arrive
        Thread.sleep(100);

        // Client should receive server-initiated message
        String serverMessage = readWebSocketTextFrame(socket);
        assertNotNull(serverMessage, "Client should receive server-initiated message");
        assertEquals("Server push message", serverMessage, "Server message should match");

        // Client responds back to verify full duplex
        sendWebSocketTextFrame(socket, "Client ack");
        assertTrue(messageReceivedLatch.await(2, TimeUnit.SECONDS), "Server should receive client ack");
        assertEquals("Client ack", receivedMessage.get(), "Server should receive client acknowledgment");

        closeWebSocketGracefully(socket);
    }

    // Helper methods

    private Socket performWebSocketHandshake(String path) throws Exception {
        Socket socket = new Socket("localhost", actualPort);
        OutputStream out = socket.getOutputStream();

        String key = generateWebSocketKey();
        String request = String.format(
            "GET %s HTTP/1.1\r\n" +
            "Host: localhost:%d\r\n" +
            "Upgrade: websocket\r\n" +
            "Connection: Upgrade\r\n" +
            "Sec-WebSocket-Key: %s\r\n" +
            "Sec-WebSocket-Version: 13\r\n" +
            "\r\n",
            path, actualPort, key
        );
        out.write(request.getBytes(StandardCharsets.UTF_8));
        out.flush();

        InputStream in = socket.getInputStream();
        byte[] buffer = new byte[1024];
        int read = in.read(buffer);
        String response = new String(buffer, 0, read, StandardCharsets.UTF_8);

        if (!response.contains("101")) {
            throw new IOException("WebSocket handshake failed: " + response);
        }

        return socket;
    }

    private String generateWebSocketKey() {
        byte[] nonce = new byte[16];
        new java.security.SecureRandom().nextBytes(nonce);
        return Base64.getEncoder().encodeToString(nonce);
    }

    private void sendWebSocketTextFrame(Socket socket, String text) throws IOException {
        OutputStream out = socket.getOutputStream();
        byte[] data = text.getBytes(StandardCharsets.UTF_8);

        // Simple WebSocket text frame (unmasked for server, masked for client)
        // FIN=1, opcode=text (0x01)
        out.write(0x81);

        if (data.length < 126) {
            out.write(0x80 | data.length); // MASK bit set + length
        } else if (data.length < 65536) {
            out.write(0x80 | 126);
            out.write((data.length >> 8) & 0xFF);
            out.write(data.length & 0xFF);
        } else {
            out.write(0x80 | 127);
            for (int i = 7; i >= 0; i--) {
                out.write((data.length >> (i * 8)) & 0xFF);
            }
        }

        // Mask key (4 bytes)
        byte[] mask = new byte[4];
        new java.security.SecureRandom().nextBytes(mask);
        out.write(mask);

        // Masked payload
        for (int i = 0; i < data.length; i++) {
            out.write(data[i] ^ mask[i % 4]);
        }

        out.flush();
    }

    private String readWebSocketTextFrame(Socket socket) throws IOException {
        socket.setSoTimeout(2000); // 2 second timeout
        InputStream in = socket.getInputStream();

        // Read first byte (FIN, RSV, opcode)
        int b1 = in.read();
        if (b1 == -1) {
            
        }

        // Read second byte (MASK, payload length)
        int b2 = in.read();
        int len = b2 & 0x7F;

        if (len == 126) {
            len = (in.read() << 8) | in.read();
        } else if (len == 127) {
            // Read 8 bytes for extended length
            len = 0;
            for (int i = 0; i < 8; i++) {
                len = (len << 8) | in.read();
            }
        }

        // Server frames are unmasked, so we just read the payload
        byte[] payload = new byte[len];
        int read = 0;
        while (read < len) {
            int r = in.read(payload, read, len - read);
            if (r == -1) break;
            read += r;
        }

        return new String(payload, StandardCharsets.UTF_8);
    }

    /**
     * Sends a proper WebSocket close frame to gracefully close the connection.
     * This prevents EOFException on the server side.
     */
    private void sendWebSocketCloseFrame(Socket socket, int code, String reason) throws IOException {
        OutputStream out = socket.getOutputStream();

        byte[] reasonBytes = reason.getBytes(StandardCharsets.UTF_8);
        byte[] payload = new byte[2 + reasonBytes.length];
        payload[0] = (byte) ((code >> 8) & 0xFF);
        payload[1] = (byte) (code & 0xFF);
        System.arraycopy(reasonBytes, 0, payload, 2, reasonBytes.length);

        // FIN=1, opcode=close (0x08)
        out.write(0x88);

        if (payload.length < 126) {
            out.write(0x80 | payload.length);
        } else if (payload.length < 65536) {
            out.write(0x80 | 126);
            out.write((payload.length >> 8) & 0xFF);
            out.write(payload.length & 0xFF);
        }

        // Mask key
        byte[] mask = new byte[4];
        new java.security.SecureRandom().nextBytes(mask);
        out.write(mask);

        // Masked payload
        for (int i = 0; i < payload.length; i++) {
            out.write(payload[i] ^ mask[i % 4]);
        }

        out.flush();
    }

    private void closeWebSocketGracefully(Socket socket) throws IOException {
        try (socket) {
            try {
                // Send close frame first
                sendWebSocketCloseFrame(socket, 1000, "Normal closure");
                // Wait a bit for server to process and respond
                Thread.sleep(100);
                // Try to read server's close response (may throw EOFException if server already closed)
                try {
                    socket.setSoTimeout(200);
                    byte[] buffer = new byte[128];
                    socket.getInputStream().read(buffer);
                } catch (IOException e) {
                    // Expected - server may have already closed
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } catch (IOException ignored) {
        }
    }
}
