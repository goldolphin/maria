package net.goldolphin.maria.api;

import java.lang.reflect.Proxy;

import net.goldolphin.maria.HttpClient;
import net.goldolphin.maria.IHttpController;

/**
 * Created by caofuxiang on 2017/3/9.
 */
public class ApiWrapper {
    public static <T> T createClientImplement(Class<T> interfaceClass, ApiClientCodec codec, HttpClient httpClient, long timeoutMs) {
        return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class<?>[] {interfaceClass},
                new ApiClientInvocationHandler(codec, httpClient, timeoutMs));
    }

    public static IHttpController createServerController(Class<?> interfaceClass, Object implement, ApiServerCodec codec) {
        return createServerController(interfaceClass, implement, new DefaultServerHandler(codec));
    }

    public static IHttpController createServerController(Class<?> interfaceClass, Object implement, ApiServerHandler handler) {
        return (pathParams, context) -> handler.invokeImplement(implement, context.getRequest(), context::send);
    }
}
