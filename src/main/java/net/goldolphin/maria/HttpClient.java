package net.goldolphin.maria;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

/**
 * @author goldolphin
 *         2016-04-04 20:45
 */
public class HttpClient {
    private static final int DEFAULT_HTTP_PORT = 80;
    private static final int DEFAULT_HTTPS_PORT = 443;
    private static final int DEFAULT_MAX_CONTENT_LENGTH = 1024 * 1024;
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    private static final SslContext SSL_CONTEXT;

    private final EventLoopGroup workerGroup;
    private final Bootstrap bootstrap;
    private final AddressResolver addressResolver;
    private final ChannelPool channelPool;

    static {
        SslContextBuilder builder = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE);
        try {
            SSL_CONTEXT = builder.build();
        } catch (SSLException e) {
            throw new IllegalStateException(e);  // Should not occur.
        }
    }

    public HttpClient() {
        this(new NioEventLoopGroup());
    }

    public HttpClient(AddressResolver addressResolver) {
        this(new NioEventLoopGroup(), addressResolver);
    }

    public HttpClient(EventLoopGroup workerGroup) {
        this(workerGroup, AddressResolver.SYSTEM_DEFAULT);
    }

    public HttpClient(EventLoopGroup workerGroup, AddressResolver addressResolver) {
        this(DEFAULT_MAX_CONTENT_LENGTH, workerGroup, addressResolver);
    }

    public HttpClient(int maxContentLength, EventLoopGroup workerGroup, AddressResolver addressResolver) {
        this(maxContentLength, workerGroup, addressResolver, 20);
    }

    public HttpClient(int maxContentLength, EventLoopGroup workerGroup, AddressResolver addressResolver, int poolCapacity) {
        this.workerGroup = workerGroup; // (1)
        bootstrap = new Bootstrap(); // (2)
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class) // (3)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("codec", new HttpClientCodec());
                        pipeline.addLast("aggregator", new HttpObjectAggregator(maxContentLength));
                    }
                }); // (4)
        this.addressResolver = addressResolver;
        this.channelPool = new ChannelPool(poolCapacity);
    }

    public CompletableFuture<FullHttpResponse> execute(HttpRequest request) {
        return execute(request, DEFAULT_TIMEOUT);
    }

    public CompletableFuture<FullHttpResponse> execute(HttpRequest request, Duration timeout) {
        String host;
        int port;
        boolean isHttps;
        try {
            HttpUtil.setKeepAlive(request, true);
            URI uri = new URI(request.uri());
            host = uri.getHost();
            String scheme = uri.getScheme().toLowerCase();
            isHttps = scheme.equals("https");
            port = uri.getPort();
            if (port < 0) {
                port = isHttps ? DEFAULT_HTTPS_PORT : DEFAULT_HTTP_PORT;
                request.headers().set(HttpHeaderNames.HOST, host);
            } else {
                request.headers().set(HttpHeaderNames.HOST, host + ":" + port);
            }

            // Refactor the URI.
            String rawPath = uri.getRawPath();
            String rawQuery = uri.getRawQuery();
            String rawFragment = uri.getRawFragment();
            StringBuilder builder = new StringBuilder(rawPath);
            if (rawQuery != null) {
                builder.append("?").append(rawQuery);
            }
            if (rawFragment != null) {
                builder.append("#").append(rawFragment);
            }
            request.setUri(builder.toString());
        } catch (URISyntaxException e) {
            CompletableFuture<FullHttpResponse> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
        // Do not throw exceptions in listeners, because they will all be swallowed by netty.
        InetSocketAddress remoteAddress = addressResolver.resolve(host, port);
        return connect(remoteAddress, isHttps, host, port).thenCompose(channel -> send(channel, request, timeout)
                .whenComplete((r, ex) -> {
                    if (ex == null && HttpUtil.isKeepAlive(request)) {
                        channelPool.release(remoteAddress, channel);
                    } else {
                        channel.close();
                    }
                }));
    }

    private CompletableFuture<FullHttpResponse> send(Channel channel, HttpRequest request, Duration timeout) {
        CompletableFuture<FullHttpResponse> future = new CompletableFuture<>();
        channel.pipeline().addLast("handler", new HttpClientHandler(future));
        channel.writeAndFlush(request).addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                if (timeout != null) {
                    f.channel().eventLoop().schedule(() -> {
                        future.completeExceptionally(new HttpTimeoutException("Time is out"));
                    }, timeout.toMillis(), TimeUnit.MILLISECONDS);
                }
            } else {
                future.completeExceptionally(f.cause());
            }
        });
        return future.thenApply(r -> {
            channel.pipeline().remove("handler");
            return r;
        });
    }

    private CompletableFuture<Channel> connect(InetSocketAddress remoteAddress, boolean isHttps, String host, int port) {
        Channel channel = channelPool.acquire(remoteAddress);
        if (channel != null && channel.isActive()) {
            return CompletableFuture.completedFuture(channel);
        }
        CompletableFuture<Channel> future = new CompletableFuture<>();
        // Do not throw exceptions in listeners, because they will all be swallowed by netty.
        bootstrap.connect(remoteAddress).addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                Channel c = f.channel();
                if (isHttps) {
                    c.pipeline().addFirst("ssl", SSL_CONTEXT.newHandler(c.alloc(), host, port));
                }
                future.complete(c);
            } else {
                future.completeExceptionally(f.cause());
            }
        });
        return future;
    }

    public void close(boolean stopEventLoop) {
        try {
            channelPool.close();
        } finally {
            if (stopEventLoop) {
                workerGroup.shutdownGracefully();
            }
        }
    }

    public EventLoopGroup getWorkerGroup() {
        return workerGroup;
    }
}
