package net.goldolphin.maria.common;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;

/**
 * @author caofuxiang
 *         2016-04-05 13:54:54.
 */
public class MessageUtils {
    public static HttpRequest newHttpRequest(HttpMethod method, String uri) {
        return new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, uri);
    }

    public static HttpRequest newHttpRequest(HttpMethod method, String uri, String content) {
        return newHttpRequest(method, uri, Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
    }

    public static HttpRequest newHttpRequest(HttpMethod method, String uri, byte[] content) {
        return newHttpRequest(method, uri, Unpooled.copiedBuffer(content));
    }

    public static HttpRequest newHttpRequest(HttpMethod method, String uri, ByteBuf content) {
        DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, uri, content);
        HttpHeaders.setContentLength(request, request.content().readableBytes());
        return request;
    }

    public static HttpResponse newHttpResponse(HttpResponseStatus status) {
        return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
    }

    public static HttpResponse newHttpResponse(HttpResponseStatus status, String contentType, String content) {
        return newHttpResponse(status, contentType, Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
    }

    public static HttpResponse newHttpResponse(HttpResponseStatus status, String contentType, byte[] content) {
        return newHttpResponse(status, contentType, Unpooled.copiedBuffer(content));
    }

    public static HttpResponse newRedirectResponse(String location) {
        HttpResponse response = newHttpResponse(HttpResponseStatus.FOUND);
        response.headers().add(HttpHeaders.Names.LOCATION, location);
        return response;
    }

    public static HttpResponse newHttpResponse(HttpResponseStatus status, String contentType, ByteBuf content) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content);
        response.headers().set(HttpHeaders.Names.CONTENT_TYPE, contentType);
        HttpHeaders.setContentLength(response, response.content().readableBytes());
        return response;
    }
}
