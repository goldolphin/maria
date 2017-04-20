package net.goldolphin.maria.api.http;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import net.goldolphin.maria.HttpContext;
import net.goldolphin.maria.IHttpController;
import net.goldolphin.maria.api.ApiHandler;
import net.goldolphin.maria.api.ApiServerCodec;

/**
 * Created by caofuxiang on 2017/4/18.
 */
public class HttpApiController<REQUEST, RESPONSE> implements IHttpController {
    private final ApiHandler<REQUEST, CompletableFuture<RESPONSE>> handler;
    private final ApiServerCodec<REQUEST, RESPONSE, FullHttpRequest, HttpResponse> codec;

    public HttpApiController(ApiHandler<REQUEST, CompletableFuture<RESPONSE>> handler,
            ApiServerCodec<REQUEST, RESPONSE, FullHttpRequest, HttpResponse> codec) {
        this.handler = handler;
        this.codec = codec;
    }

    @Override
    public void handle(Map<String, String> pathParams, HttpContext context) throws Exception {
        REQUEST request = codec.decodeRequest(context.getRequest());
        try {
            handler.call(request)
                    .thenAccept(response -> context.send(codec.encodeResponse(response)))
                    .exceptionally(e -> {
                        context.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR);
                        return null;
                    });
        } catch (Exception e) {
            context.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
