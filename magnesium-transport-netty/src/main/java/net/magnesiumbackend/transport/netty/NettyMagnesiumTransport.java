package net.magnesiumbackend.transport.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import net.magnesiumbackend.core.MagnesiumRuntime;
import net.magnesiumbackend.core.backpressure.BackpressureExecutorResolver;
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
import java.util.concurrent.Executor;

import static io.netty.channel.nio.NioIoHandler.newFactory;

public class NettyMagnesiumTransport implements MagnesiumTransport {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyMagnesiumTransport.class);

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel channel;

    @Override
    public void bind(int port, MagnesiumRuntime runtime, HttpRouteRegistry routes) {
        bossGroup  = new MultiThreadIoEventLoopGroup(newFactory());
        workerGroup = new MultiThreadIoEventLoopGroup(newFactory());

        SslConfig  sslConfig = runtime.sslConfig();
        SslContext nettyCtx  = buildSslContext(sslConfig);

        // When BackpressureConfig is present this returns a BoundedBackpressureExecutor
        // wrapping the user's executor. Otherwise, it returns the raw executor unchanged.
        // NettyPipelineFactory (and ultimately NettyHttpServerHandler) receive this
        // executor, no other transport code needs to know about backpressure.
        Executor requestExecutor = BackpressureExecutorResolver.resolve(runtime);

        NettyPipelineFactory factory = new NettyPipelineFactory(
            routes,
            runtime.router().globalFilters(),
            runtime.exceptionHandlerRegistry(),
            runtime.messageConverterRegistry(),
            runtime.router().webSocketRouteRegistry(),
            runtime.router().webSocketSessionManager(),
            sslConfig,
            runtime.securityHeadersFilter(),
            requestExecutor,
            runtime.defaultTimeout()
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

            try {
                runtime.application().start(runtime);
            } catch (Exception e) {
                throw new IllegalStateException("Application failed during start()", e);
            }

            this.channel = bootstrap.bind(port).sync().channel();
            LOGGER.info("[Magnesium] Netty listening on port {} ({})",
                port, sslConfig != null ? "HTTPS" : "HTTP");

            try {
                try {
                    runtime.application().ready(runtime, getPort());
                } catch (Exception e) {
                    throw new RuntimeException("[Magnesium] Netty transport interrupted by an error from Application#ready.", e);
                }
                runtime.shutdownLatch().await();
                channel.close().sync();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
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
            throw new IllegalStateException("[Magnesium] Failed to initialize Netty SSL context.", e);
        }
    }

    @Override
    public int getPort() {
        SocketAddress addr = channel.localAddress();
        if (addr == null) throw new IllegalStateException("Server not started");
        return ((InetSocketAddress) addr).getPort();
    }

    @Override
    public TransportExecutionModel executionModel() {
        return TransportExecutionModel.NON_BLOCKING;
    }
}