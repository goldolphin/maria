package net.goldolphin.maria.api.protoson;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.CharsetUtil;
import net.goldolphin.maria.api.ApiServerCodec;

/**
 * Created by caofuxiang on 2017/8/16.
 */
public class HttpCliCodec implements ApiServerCodec<HttpRequest, FullHttpResponse, String[], String> {
    private final String serviceBase;

    private HttpCliCodec(String serviceBase) {
        this.serviceBase = serviceBase;
    }

    @Override
    public HttpRequest decodeRequest(String[] encoded) {
        String method;
        String content;
        if (encoded.length >= 2) {
            method = encoded[0];
            content = encoded[1];
        } else {
            method = encoded[0];
            content = null;
        }
        return HttpClientCodec.encodeRequest(serviceBase, method, content);
    }

    @Override
    public String encodeResponse(FullHttpResponse encoded) {
        return encoded.content().toString(CharsetUtil.UTF_8);
    }

    public static HttpCliCodec create(String serviceBase) {
        return new HttpCliCodec(serviceBase);
    }
}
