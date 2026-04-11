module magnesium.transport.tomcat {
    requires core;
    requires org.apache.tomcat.embed.core;
    requires org.slf4j;
    requires org.jetbrains.annotations;
    requires org.apache.tomcat.embed.websocket;

    provides net.magnesiumbackend.core.http.MagnesiumTransport
        with net.magnesiumbackend.transport.tomcat.TomcatMagnesiumTransport;

    exports net.magnesiumbackend.transport.tomcat;
}