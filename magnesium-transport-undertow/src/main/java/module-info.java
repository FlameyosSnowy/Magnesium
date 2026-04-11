module magnesium.transport.undertow {
    requires core;
    requires org.slf4j;
    requires undertow.core;
    requires org.jetbrains.annotations;
    requires xnio.api;

    provides net.magnesiumbackend.core.http.MagnesiumTransport
        with net.magnesiumbackend.transport.undertow.UndertowMagnesiumTransport;

    exports net.magnesiumbackend.transport.undertow;
}