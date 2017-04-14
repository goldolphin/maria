package net.goldolphin.maria.api;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponse;

/**
 * Created by caofuxiang on 2017/4/15.
 */
public class DefaultServerHandler implements ApiServerHandler {
    private final ApiServerCodec codec;

    public DefaultServerHandler(ApiServerCodec codec) {
        this.codec = codec;
    }

    @Override
    public void invokeImplement(Object implement, FullHttpRequest httpRequest, Consumer<HttpResponse> continuation) {
        ApiServerCodec.DecodeResult decodeResult = codec.decodeRequest(httpRequest);
        if (decodeResult.getErrorResponse() != null) {
            continuation.accept(codec.encodeResponse(decodeResult.getErrorResponse()));
            return;
        }

        Method method = decodeResult.getMethod();
        try {
            Object ret = method.invoke(implement, decodeResult.getArgs());
            if (ret instanceof CompletableFuture) {
                ((CompletableFuture<?>) ret).thenAccept(response -> continuation.accept(codec.encodeResponse(response)));
            } else {
                continuation.accept(codec.encodeResponse(ret));
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
