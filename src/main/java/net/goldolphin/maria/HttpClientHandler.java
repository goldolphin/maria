package net.goldolphin.maria;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;

/**
 * @author caofuxiang
 *         2016-04-05 10:27:27.
 */
public class HttpClientHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
    private static final Logger logger = LoggerFactory.getLogger(HttpClientHandler.class);
    private final CompletableFuture<FullHttpResponse> future;

    public HttpClientHandler(CompletableFuture<FullHttpResponse> future) {
        this.future = future;
    }

    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) throws Exception {
        // Ensure that we copied a response with unpooled buffer.
        DefaultFullHttpResponse copy = new DefaultFullHttpResponse(
                response.getProtocolVersion(),
                response.getStatus(),
                Unpooled.copiedBuffer(response.content()));
        copy.headers().set(response.headers());
        copy.trailingHeaders().set(response.trailingHeaders());
        future.complete(copy);
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        future.completeExceptionally(cause);
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        future.completeExceptionally(new NoHttpResponseException("Server failed to respond"));
        super.channelInactive(ctx);
    }
}
