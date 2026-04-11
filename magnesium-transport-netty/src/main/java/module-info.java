module magnesium.transport.netty {
    requires io.netty.buffer;
    requires io.netty.codec.http;
    requires io.netty.transport;
    requires org.jetbrains.annotations;
    requires org.slf4j;
    requires core;
    requires io.netty.common;
    requires io.github.flameyossnowy.velocis;
    requires io.netty.handler;
    requires io.netty.pkitesting;
    requires io.netty.codec.http2;

    provides net.magnesiumbackend.core.http.MagnesiumTransport
        with net.magnesiumbackend.transport.netty.NettyMagnesiumTransport;

    exports net.magnesiumbackend.transport.netty;
    exports net.magnesiumbackend.transport.netty.websocket;
    exports net.magnesiumbackend.transport.netty.handler;
    exports net.magnesiumbackend.transport.netty.adapter;
    exports net.magnesiumbackend.transport.netty.pipeline;
}