package net.goldolphin.maria.restful;

import net.goldolphin.maria.HttpContext;
import net.goldolphin.maria.IHttpController;
import io.netty.handler.codec.http.HttpMethod;

import java.io.IOException;
import java.util.Map;

/**
 * @author caofuxiang
 *         2014-03-05 15:32
 */
public class RestfulController implements IHttpController {
    protected void post(Map<String, String> pathParams, HttpContext context) throws Exception {
        context.sendNotFound();
    }

    protected void get(Map<String, String> pathParams, HttpContext context) throws Exception {
        context.sendNotFound();
    }

    protected void put(Map<String, String> pathParams, HttpContext context) throws Exception {
        context.sendNotFound();
    }

    protected void patch(Map<String, String> pathParams, HttpContext context) throws Exception {
        context.sendNotFound();
    }

    protected void delete(Map<String, String> pathParams, HttpContext context) throws Exception {
        context.sendNotFound();
    }

    protected void options(Map<String, String> pathParams, HttpContext context) throws Exception {
        context.sendNotFound();
    }

    public void handle(Map<String, String> pathParams, HttpContext context) throws Exception {
        HttpMethod method = context.getRequest().getMethod();
        if (method.equals(HttpMethod.POST)) {
            post(pathParams, context);
        } else if (method.equals(HttpMethod.GET)) {
            get(pathParams, context);
        } else if (method.equals(HttpMethod.PUT)) {
            put(pathParams, context);
        } else if (method.equals(HttpMethod.PATCH)) {
            patch(pathParams, context);
        } else if (method.equals(HttpMethod.DELETE)) {
            delete(pathParams, context);
        } else if (method.equals(HttpMethod.OPTIONS)) {
            options(pathParams, context);
        } else {
            throw new IOException("Unsupported method: " + method);
        }
    }
}
