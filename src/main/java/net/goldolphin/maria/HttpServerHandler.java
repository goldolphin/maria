package net.goldolphin.maria;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author caofuxiang
 *         2014-03-04 14:30
 */
@ChannelHandler.Sharable
public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger logger = LoggerFactory.getLogger(HttpServerHandler.class);
    private final HttpDispatcher dispatcher;

    public HttpServerHandler(HttpDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        HttpContext httpContext = new HttpContext(request, ctx);
        try {
            dispatcher.dispatch(httpContext);
        } catch (Exception e) {
            logger.warn("Controller exception caught.", e);
            httpContext.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        logger.error("close connection with exception.", cause);
        ctx.close();
    }
}
