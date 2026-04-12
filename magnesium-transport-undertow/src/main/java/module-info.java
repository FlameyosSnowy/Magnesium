module magnesium.transport.undertow {
    requires core;
    requires org.slf4j;
    requires undertow.core;
    requires org.jetbrains.annotations;
    requires xnio.api;
    requires io.github.flameyossnowy.velocis;

    provides net.magnesiumbackend.core.http.MagnesiumTransport
        with net.magnesiumbackend.transport.undertow.UndertowMagnesiumTransport;

    provides net.magnesiumbackend.core.http.websocket.WebSocketSessionManager
        with net.magnesiumbackend.transport.undertow;

    exports net.magnesiumbackend.transport.undertow;
    exports net.magnesiumbackend.transport.undertow.websocket;
    exports net.magnesiumbackend.transport.undertow.adapter;
    exports net.magnesiumbackend.transport.undertow.handler;
}