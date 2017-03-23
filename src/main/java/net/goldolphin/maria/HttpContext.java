package net.goldolphin.maria;

import net.goldolphin.maria.common.MessageUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

/**
 * @author caofuxiang
 *         2014-03-05 14:54
 */
public class HttpContext {
    private final ChannelHandlerContext underlyingContext;
    private final FullHttpRequest request;

    public HttpContext(FullHttpRequest request, ChannelHandlerContext underlyingContext) {
        this.request = request;
        this.underlyingContext = underlyingContext;
    }

    public FullHttpRequest getRequest() {
        return request;
    }

    public ChannelHandlerContext getUnderlyingContext() {
        return underlyingContext;
    }

    public void send(HttpResponse res) {
        ChannelFuture f = underlyingContext.writeAndFlush(res);
        if (!(HttpHeaders.isKeepAlive(request) && res.getStatus().code() == OK.code())) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }

    public void send(String contentType, ByteBuf content) {
        send(MessageUtils.newHttpResponse(OK, contentType, content));
    }

    public void send(String contentType, byte[] content) {
        send(MessageUtils.newHttpResponse(OK, contentType, content));
    }

    public void send(String content) {
        send(MessageUtils.newHttpResponse(OK, "text/html; charset=UTF-8", content));
    }

    public void sendRedirect(String location) {
        send(MessageUtils.newRedirectResponse(location));
    }

    public void sendError(HttpResponseStatus status) {
        send(MessageUtils.newHttpResponse(status, "text/html; charset=UTF-8", status.toString()));
    }

    public void sendNotFound() {
        sendError(NOT_FOUND);
    }
}
