package net.goldolphin.maria;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

/**
 * @author goldolphin
 *         2016-04-04 18:07
 */
public class HttpServer {
    private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);

    private final SocketAddress localAddress;
    private final HttpServerHandler handler;

    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private final ServerBootstrap bootstrap;
    private ChannelFuture future = null;

    public HttpServer(SocketAddress localAddress, HttpDispatcher dispatcher) {
        this(localAddress, dispatcher, 1024*1024);
    }

    public HttpServer(SocketAddress localAddress, HttpDispatcher dispatcher, int maxContentLength) {
        this(localAddress, dispatcher, maxContentLength, new NioEventLoopGroup(1), new NioEventLoopGroup());
    }

    public HttpServer(SocketAddress localAddress, HttpDispatcher dispatcher, int maxContentLength, EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
        this.localAddress = localAddress;
        this.handler = new HttpServerHandler(dispatcher);
        this.bossGroup = bossGroup; // (1)
        this.workerGroup = workerGroup;

        bootstrap = new ServerBootstrap(); // (2)
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class) // (3)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("codec", new HttpServerCodec());
                        pipeline.addLast("aggregator", new HttpObjectAggregator(maxContentLength));
                        pipeline.addLast("handler", handler);
                    }
                }) // (4)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.SO_BACKLOG, 128);          // (5)
                // .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)
    }

    public void start() {
        //bind and start to accept incoming connections.
        logger.info("Start HTTP Service at: " + localAddress);
        future = bootstrap.bind(localAddress); // (7)
        future.syncUninterruptibly();
    }

    public void stop(boolean stopEventLoop) throws InterruptedException {
        if (future != null) {
            try {
                logger.info("Shutdown HTTP Service at: " + localAddress);
                future.channel().close().syncUninterruptibly();
            } finally {
                if (stopEventLoop) {
                    bossGroup.shutdownGracefully();
                    workerGroup.shutdownGracefully();
                }
                future = null;
            }
        }
    }

    public void await() {
        future.channel().closeFuture().syncUninterruptibly();
    }

    public SocketAddress getLocalAddress() {
        return localAddress;
    }

    public EventLoopGroup getBossGroup() {
        return bossGroup;
    }

    public EventLoopGroup getWorkerGroup() {
        return workerGroup;
    }
}
