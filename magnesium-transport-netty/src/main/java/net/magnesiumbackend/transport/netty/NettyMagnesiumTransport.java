package net.magnesiumbackend.transport.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.*;
import net.magnesiumbackend.core.MagnesiumApplication;
import net.magnesiumbackend.core.http.MagnesiumTransport;
import net.magnesiumbackend.core.http.TransportExecutionModel;
import net.magnesiumbackend.core.route.HttpRouteRegistry;
import net.magnesiumbackend.core.security.SslConfig;
import net.magnesiumbackend.transport.netty.adapter.NettySslAdapter;
import net.magnesiumbackend.transport.netty.pipeline.NettyPipelineFactory;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import static io.netty.channel.nio.NioIoHandler.newFactory;

public class NettyMagnesiumTransport implements MagnesiumTransport {
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyMagnesiumTransport.class);

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel channel;

    @Override
    public void bind(int port, MagnesiumApplication application, HttpRouteRegistry routes) {
        bossGroup  = new MultiThreadIoEventLoopGroup(newFactory());
        workerGroup = new MultiThreadIoEventLoopGroup(newFactory());

        SslConfig sslConfig = application.sslConfig();

        SslContext nettyCtx = buildSslContext(sslConfig);

        NettyPipelineFactory factory = new NettyPipelineFactory(
            routes,
            application.httpServer().globalFilters(),
            application.exceptionHandlerRegistry(),
            application.messageConverterRegistry(),
            application.httpServer().webSocketRouteRegistry(),
            application.httpServer().webSocketSessionManager(),
            sslConfig,
            application.securityHeadersFilter(),
            application.executor()
        );

        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        factory.initChannel(ch.pipeline(), nettyCtx);
                    }
                });

            this.channel = bootstrap.bind(port).sync().channel();
            LOGGER.info("[Magnesium] Netty listening on port {} ({})",
                port, sslConfig != null ? "HTTPS" : "HTTP");

            application.onStart().accept(application.serviceRegistry());
            application.shutdownLatch().await();
            channel.close().sync();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("[Magnesium] Netty transport interrupted.", e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    @Override
    public void shutdown() {
        if (bossGroup  != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
    }

    @Nullable
    private SslContext buildSslContext(@Nullable SslConfig sslConfig) {
        if (sslConfig == null) return null;
        try {
            return NettySslAdapter.toNettyContext(sslConfig);
        } catch (Exception e) {
            throw new IllegalStateException(
                "[Magnesium] Failed to initialize Netty SSL context.", e
            );
        }
    }

    @Override
    public int getPort() {
        SocketAddress socketAddress = channel.localAddress();
        if (socketAddress == null) {
            throw new IllegalStateException("Server not started");
        }
        return ((InetSocketAddress) socketAddress).getPort();
    }

    @Override
    public TransportExecutionModel executionModel() {
        return TransportExecutionModel.NON_BLOCKING;
    }
}
