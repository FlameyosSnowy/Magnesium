module magnesium.transport.httpserver {
    requires core;
    requires jdk.httpserver;
    requires org.slf4j;
    requires robaho.httpserver;
    requires org.jetbrains.annotations;

    provides net.magnesiumbackend.core.http.MagnesiumTransport
        with net.magnesiumbackend.transport.httpserver.HttpServerMagnesiumTransport;

    exports net.magnesiumbackend.transport.httpserver;
}