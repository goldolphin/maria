package net.goldolphin.maria.api.http;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import net.goldolphin.maria.HttpContext;
import net.goldolphin.maria.IHttpController;
import net.goldolphin.maria.api.ApiHandler;

/**
 * Created by caofuxiang on 2017/4/18.
 */
public class HttpApiController implements IHttpController {
    private final ApiHandler<HttpContext, CompletableFuture<HttpResponse>> handler;

    public HttpApiController(ApiHandler<HttpContext, CompletableFuture<HttpResponse>> handler) {
        this.handler = handler;
    }

    @Override
    public void handle(Map<String, String> pathParams, HttpContext context) throws Exception {
        try {
            handler.call(context)
                    .thenAccept(context::send)
                    .exceptionally(e -> {
                        context.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR);
                        return null;
                    });
        } catch (Exception e) {
            context.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
