package net.goldolphin.maria;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.CharsetUtil;
import net.goldolphin.maria.common.MessageUtils;

/**
 * @author goldolphin
 *         2016-04-04 20:45
 */
public class HttpClient {
    private static final Logger logger = LoggerFactory.getLogger(HttpClient.class);

    private static final SslContext SSL_CONTEXT;
    private final EventLoopGroup workerGroup;
    private final Bootstrap bootstrap;

    static {
        SslContextBuilder builder = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE);
        try {
            SSL_CONTEXT = builder.build();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
    }

    public HttpClient() {
        this(new NioEventLoopGroup());
    }

    public HttpClient(EventLoopGroup workerGroup) {
        this(1024*1024, workerGroup);
    }

    public HttpClient(int maxContentLength, EventLoopGroup workerGroup) {
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
    }

    public CompletableFuture<FullHttpResponse> execute(HttpRequest request) {
        return execute(request, 10, TimeUnit.SECONDS);
    }

    public CompletableFuture<FullHttpResponse> execute(HttpRequest request, final long timeout, final TimeUnit unit) {
        final CompletableFuture<FullHttpResponse> future = new CompletableFuture<>();
        String host;
        int port;
        boolean isHttps;
        try {
            URI uri = new URI(request.getUri());
            host = uri.getHost();
            String scheme = uri.getScheme().toLowerCase();
            isHttps = scheme.equals("https");
            port = uri.getPort();
            if (port < 0) {
                port = isHttps ? 443 : 80;
                HttpHeaders.setHost(request, host);
            } else {
                HttpHeaders.setHost(request, host + ":" + port);
            }

            // Refactor the URI.
            String rawPath = uri.getRawPath();
            String rawQuery = uri.getRawQuery();
            String rawFragment = uri.getRawFragment();
            StringBuilder builder = new StringBuilder(rawPath);
            if (rawQuery != null) builder.append("?").append(rawQuery);
            if (rawFragment != null) builder.append("#").append(rawFragment);
            request.setUri(builder.toString());
        } catch (URISyntaxException e) {
            future.completeExceptionally(e);
            return future;
        }
        bootstrap.connect(host, port).addListener(new ChannelFutureListener() {
            // Do not throw exceptions in listeners, because they will all be swallowed by netty.
            public void operationComplete(ChannelFuture f) {
                if (f.isSuccess()) {
                    final Channel channel = f.channel();
                    if (isHttps) {
                        channel.pipeline().addFirst("ssl", SSL_CONTEXT.newHandler(channel.alloc()));
                    }
                    channel.pipeline().addLast("handler", new HttpClientHandler(future));
                    channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
                        public void operationComplete(ChannelFuture f) {
                            if (f.isSuccess()) {
                                if (timeout > 0) {
                                    f.channel().eventLoop().schedule(() -> {
                                        future.completeExceptionally(new HttpTimeoutException("Time is out"));
                                        channel.close();
                                    }, timeout, unit);
                                }
                            } else {
                                future.completeExceptionally(f.cause());
                                channel.close();
                            }
                        }
                    });
                } else {
                    future.completeExceptionally(f.cause());
                }
            }
        });
        return future;
    }

    public EventLoopGroup getWorkerGroup() {
        return workerGroup;
    }

    public static void main(String[] args) throws URISyntaxException, ExecutionException, InterruptedException {
        HttpClient client = new HttpClient();
        HttpRequest request = MessageUtils.newHttpRequest(HttpMethod.GET, "https://account.xiaomi.com/");
        FullHttpResponse response = client.execute(request, 10, TimeUnit.SECONDS).get();
        System.out.println(response);
        System.out.println(response.content().toString(CharsetUtil.UTF_8));
        client.getWorkerGroup().shutdownGracefully();
    }
}
