module magnesium.transport.httpserver {
    requires core;
    requires jdk.httpserver;
    requires org.slf4j;
    requires robaho.httpserver;
    requires org.jetbrains.annotations;

    provides net.magnesiumbackend.core.http.MagnesiumTransport
        with net.magnesiumbackend.transport.httpserver.HttpServerMagnesiumTransport;

    provides net.magnesiumbackend.core.http.websocket.WebSocketSessionManager
        with net.magnesiumbackend.transport.httpserver.InMemoryWebSocketSessionManager;

    exports net.magnesiumbackend.transport.httpserver;
}