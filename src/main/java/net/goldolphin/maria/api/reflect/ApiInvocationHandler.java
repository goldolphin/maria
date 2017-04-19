package net.goldolphin.maria.api.reflect;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import net.goldolphin.maria.api.ApiClientCodec;
import net.goldolphin.maria.api.ApiHandler;

/**
 * Created by caofuxiang on 2017/4/19.
 */
public class ApiInvocationHandler<REQUEST, RESPONSE> implements InvocationHandler {
    private final ApiHandler<REQUEST, RESPONSE> handler;
    private final ApiClientCodec<MethodAndArgs, Object, REQUEST, RESPONSE> codec;

    public ApiInvocationHandler(ApiHandler<REQUEST, RESPONSE> handler, ApiClientCodec<MethodAndArgs, Object, REQUEST, RESPONSE> codec) {
        this.handler = handler;
        this.codec = codec;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        MethodAndArgs methodAndArgs = new MethodAndArgs(method, args);
        RESPONSE response = handler.call(codec.encodeRequest(methodAndArgs));
        return codec.decodeResponse(methodAndArgs, response);
    }
}
