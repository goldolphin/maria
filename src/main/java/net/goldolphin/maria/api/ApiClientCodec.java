package net.goldolphin.maria.api;

import java.io.IOException;
import java.lang.reflect.Method;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;

/**
 * Created by caofuxiang on 2017/4/14.
 */
public interface ApiClientCodec {
    HttpRequest encodeRequest(Method method, Object[] args);
    Object decodeResponse(Method method, FullHttpResponse httpResponse) throws IOException;
}
