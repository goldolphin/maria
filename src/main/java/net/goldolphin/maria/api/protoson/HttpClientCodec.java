package net.goldolphin.maria.api.protoson;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.CharsetUtil;
import net.goldolphin.maria.api.ApiClientCodec;
import net.goldolphin.maria.common.MessageUtils;
import net.goldolphin.maria.common.UrlUtils;

/**
 * Created by caofuxiang on 2017/4/19.
 */
public class HttpClientCodec implements ApiClientCodec<Request, Response, HttpRequest, FullHttpResponse> {
    private final String serviceBase;

    public HttpClientCodec(String serviceBase) {
        this.serviceBase = serviceBase;
    }

    @Override
    public HttpRequest encodeRequest(Request request) {
        String path = UrlUtils.concat(serviceBase, request.getMethod());
        HttpRequest httpRequest;
        if (request.getContent() != null) {
            httpRequest = MessageUtils.newHttpRequest(HttpMethod.POST, path, request.getContent());
        } else {
            httpRequest = MessageUtils.newHttpRequest(HttpMethod.POST, path);
        }
        return httpRequest;
    }

    @Override
    public Response decodeResponse(Request request, FullHttpResponse encoded) {
        return new Response(encoded.content().toString(CharsetUtil.UTF_8));
    }
}
