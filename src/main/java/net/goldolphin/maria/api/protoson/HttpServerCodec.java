package net.goldolphin.maria.api.protoson;

import java.net.URI;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;
import net.goldolphin.maria.api.ApiServerCodec;
import net.goldolphin.maria.common.MessageUtils;
import net.goldolphin.maria.common.UrlUtils;

/**
 * Created by caofuxiang on 2017/4/19.
 */
public class HttpServerCodec implements ApiServerCodec<Request, Response, FullHttpRequest, HttpResponse> {
    @Override
    public Request decodeRequest(FullHttpRequest encoded) {
        URI uri = URI.create(encoded.getUri());
        String method = UrlUtils.getBasename(uri.getPath());
        return new Request(method, encoded.content().toString(CharsetUtil.UTF_8));
    }

    @Override
    public HttpResponse encodeResponse(Response s) {
        return MessageUtils.newHttpResponse(HttpResponseStatus.OK, "text/json;charset=UTF-8;", s.getContent());
    }
}
