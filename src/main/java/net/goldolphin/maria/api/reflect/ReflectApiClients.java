package net.goldolphin.maria.api.reflect;

import java.lang.reflect.Proxy;

import net.goldolphin.maria.api.ApiClientCodec;
import net.goldolphin.maria.api.ApiHandler;

/**
 * Created by caofuxiang on 2017/4/19.
 */
public class ReflectApiClients {
    public static <T, REQUEST, RESPONSE> T createClient(Class<T> interfaceClass, ApiHandler<REQUEST, RESPONSE> handler,
            ApiClientCodec<MethodAndArgs, Object, REQUEST, RESPONSE> codec) {
        return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class<?>[] {interfaceClass},
                new ApiInvocationHandler(handler, codec));
    }
}
