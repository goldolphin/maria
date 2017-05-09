package net.goldolphin.maria.api.http;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import net.goldolphin.maria.HttpClient;
import net.goldolphin.maria.api.ApiClientCodec;
import net.goldolphin.maria.api.ApiHandler;
import net.goldolphin.maria.api.InvalidHttpStatusException;

/**
 * Created by caofuxiang on 2017/4/19.
 */
public class HttpApiClientHandler<REQUEST, RESPONSE> implements ApiHandler<REQUEST, CompletableFuture<RESPONSE>> {
    private final ApiClientCodec<REQUEST, RESPONSE, HttpRequest, FullHttpResponse> codec;
    private final HttpClient httpClient;
    private final long timeout;
    private final TimeUnit unit;

    public HttpApiClientHandler(ApiClientCodec<REQUEST, RESPONSE, HttpRequest, FullHttpResponse> codec,
            HttpClient httpClient, long timeout, TimeUnit unit) {
        this.codec = codec;
        this.httpClient = httpClient;
        this.timeout = timeout;
        this.unit = unit;
    }

    @Override
    public CompletableFuture<RESPONSE> call(REQUEST request) {
        return httpClient.execute(codec.encodeRequest(request), timeout, unit)
                .thenApply(response -> {
                    if (response.getStatus().equals(HttpResponseStatus.OK)) {
                        return codec.decodeResponse(request, response);
                    }
                    throw new InvalidHttpStatusException(response.getStatus());
                });
    }
}
