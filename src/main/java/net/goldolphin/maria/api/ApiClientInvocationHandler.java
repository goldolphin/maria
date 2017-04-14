package net.goldolphin.maria.api;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import net.goldolphin.maria.HttpClient;

/**
 * Created by caofuxiang on 2017/4/15.
 */
public class ApiClientInvocationHandler implements InvocationHandler {
    private final ApiClientCodec codec;
    private final HttpClient httpClient;
    private final long timeoutMs;

    public ApiClientInvocationHandler(ApiClientCodec codec, HttpClient httpClient, long timeoutMs) {
        this.codec = codec;
        this.httpClient = httpClient;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        HttpRequest httpRequest = codec.encodeRequest(method, args);
        return httpClient.execute(httpRequest, timeoutMs, TimeUnit.MILLISECONDS)
                .thenApply(response -> {
                    if (response.getStatus().equals(HttpResponseStatus.OK)) {
                        try {
                            return codec.decodeResponse(method, response);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    throw new RuntimeException("Service Unavailable");
                });
    }
}
