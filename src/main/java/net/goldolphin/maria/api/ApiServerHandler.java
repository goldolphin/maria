package net.goldolphin.maria.api;

import java.util.function.Consumer;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponse;

/**
 * Created by caofuxiang on 2017/4/15.
 */
public interface ApiServerHandler {
    void invokeImplement(Object implement, FullHttpRequest httpRequest, Consumer<HttpResponse> continuation);
}
