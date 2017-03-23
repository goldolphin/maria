package net.goldolphin.maria;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * An asynchronous controller.
 * @author caofuxiang
 *         2016-07-04 20:10
 */
public abstract class AsyncHttpController implements IHttpController {
    private static final Logger logger = LoggerFactory.getLogger(AsyncHttpController.class);

    @Override
    public void handle(Map<String, String> pathParams, HttpContext context) throws Exception {
        CompletableFuture<Void> future = handleAsync(pathParams, context);
        if (future != null) {
            future.exceptionally(e -> {
                logger.warn("Controller exception caught.", e);
                context.sendError(HttpResponseStatus.INTERNAL_SERVER_ERROR);
                return null;
            });
        }
    }

    /**
     * Returns a future representing the action, or null for no operation will be taken.
     * @param pathParams
     * @param context
     * @return
     * @throws Exception
     */
    protected abstract CompletableFuture<Void> handleAsync(Map<String, String> pathParams, HttpContext context) throws Exception;
}
