package net.magnesiumbackend.transport.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import net.magnesiumbackend.core.MagnesiumApplication;
import net.magnesiumbackend.core.registry.HttpRouteRegistry;

public class MagnesiumNettyServer {
    private final int port;
    private final HttpRouteRegistry httpRouteRegistry;
    private final MagnesiumApplication application;

    public MagnesiumNettyServer(int port, HttpRouteRegistry httpRouteRegistry, MagnesiumApplication application) {
        this.port = port;
        this.httpRouteRegistry = httpRouteRegistry;
        this.application = application;
    }

    public void run() throws InterruptedException {
        MultiThreadIoEventLoopGroup bossGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        MultiThreadIoEventLoopGroup childGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, childGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new NettySocketChannelInitializer(
                    httpRouteRegistry,
                    application.httpServer().webSocketRouteRegistry(),
                    application.exceptionHandlerRegistry(),
                    application.messageConverterRegistry(),
                    application,
                    application.sslConfig()
                ))
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture future = bootstrap.bind(port).sync();
            this.application.onStart().accept(this.application.serviceRegistry());
            this.application.shutdownLatch().await();

            future.channel().close().sync();
        } finally {
            childGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }
}
