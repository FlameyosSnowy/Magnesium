module magnesium.transport.tomcat {
    requires core;
    requires org.apache.tomcat.embed.core;
    requires org.slf4j;
    requires org.jetbrains.annotations;
    requires org.apache.tomcat.embed.websocket;
    requires io.github.flameyossnowy.velocis;

    provides net.magnesiumbackend.core.http.MagnesiumTransport
        with net.magnesiumbackend.transport.tomcat.TomcatMagnesiumTransport;

    provides net.magnesiumbackend.core.http.websocket.WebSocketSessionManager
        with net.magnesiumbackend.transport.tomcat.TomcatWebSocketSessionManager;

    exports net.magnesiumbackend.transport.tomcat;
}