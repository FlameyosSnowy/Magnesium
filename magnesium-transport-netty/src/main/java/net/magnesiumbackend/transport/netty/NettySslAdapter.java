package net.magnesiumbackend.transport.netty;

import io.netty.handler.ssl.*;
import net.magnesiumbackend.core.security.SslConfig;

public final class NettySslAdapter {

    private NettySslAdapter() {}

    /**
     * Builds a Netty SslContext from a transport-agnostic SslConfig.
     * Advertises HTTP/2 (h2) and HTTP/1.1 via ALPN so the negotiator
     * can route each connection to the correct pipeline.
     * Falls back to HTTP/1.1 only if SSL is present but no HTTP/2 support is needed.
     */
    public static SslContext toNettyContext(SslConfig config) throws Exception {
        return SslContextBuilder
            .forServer(config.keyManagerFactory())
            .sslProvider(SslProvider.JDK)
            .applicationProtocolConfig(new ApplicationProtocolConfig(
                ApplicationProtocolConfig.Protocol.ALPN,
                ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                ApplicationProtocolNames.HTTP_2,
                ApplicationProtocolNames.HTTP_1_1
            ))
            .build();
    }
}
