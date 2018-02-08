package net.goldolphin.maria.api.http;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import net.goldolphin.maria.HttpClient;
import net.goldolphin.maria.api.ApiHandler;
import net.goldolphin.maria.api.InvalidHttpStatusException;

/**
 * Created by caofuxiang on 2017/4/19.
 */
public class HttpApiClientHandler implements ApiHandler<HttpRequest, CompletableFuture<FullHttpResponse>> {
    private final HttpClient httpClient;
    private final Duration timeout;

    public HttpApiClientHandler(HttpClient httpClient, Duration timeout) {
        this.httpClient = httpClient;
        this.timeout = timeout;
    }

    @Override
    public CompletableFuture<FullHttpResponse> call(HttpRequest request) {
        return httpClient.execute(request, timeout)
                .thenApply(response -> {
                    if (response.getStatus().equals(HttpResponseStatus.OK)) {
                        return response;
                    }
                    throw new InvalidHttpStatusException(response.getStatus());
                });
    }
}
