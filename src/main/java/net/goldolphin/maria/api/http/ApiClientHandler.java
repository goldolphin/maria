package net.goldolphin.maria.api.http;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import net.goldolphin.maria.HttpClient;
import net.goldolphin.maria.api.ApiClientCodec;
import net.goldolphin.maria.api.ApiHandler;

/**
 * Created by caofuxiang on 2017/4/19.
 */
public class ApiClientHandler<REQUEST, RESPONSE> implements ApiHandler<REQUEST, CompletableFuture<RESPONSE>> {
    private final ApiClientCodec<REQUEST, RESPONSE, HttpRequest, FullHttpResponse> codec;
    private final HttpClient httpClient;
    private final long timeoutMs;

    public ApiClientHandler(ApiClientCodec<REQUEST, RESPONSE, HttpRequest, FullHttpResponse> codec, HttpClient httpClient, long timeoutMs) {
        this.codec = codec;
        this.httpClient = httpClient;
        this.timeoutMs = timeoutMs;
    }

    @Override
    public CompletableFuture<RESPONSE> call(REQUEST request) {
        return httpClient.execute(codec.encodeRequest(request), timeoutMs, TimeUnit.MILLISECONDS)
                .thenApply(response -> {
                    if (response.getStatus().equals(HttpResponseStatus.OK)) {
                        return codec.decodeResponse(request, response);
                    }
                    throw new RuntimeException("Service Unavailable");
                });
    }
}
