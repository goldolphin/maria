package net.goldolphin.maria.api.reflect;

import net.goldolphin.maria.api.ApiHandler;
import net.goldolphin.maria.api.ApiServerCodec;

/**
 * Created by caofuxiang on 2017/4/19.
 */
public class ReflectServerHandler<REQUEST, RESPONSE> implements ApiHandler<REQUEST, RESPONSE> {
    private final Object implement;
    private final ApiServerCodec<MethodAndArgs, Object, REQUEST, RESPONSE> codec;
    private final ErrorHandler<RESPONSE> errorHandler;

    public ReflectServerHandler(Object implement, ApiServerCodec<MethodAndArgs, Object, REQUEST, RESPONSE> codec,
            ErrorHandler<RESPONSE> errorHandler) {
        this.implement = implement;
        this.codec = codec;
        this.errorHandler = errorHandler;
    }

    @Override
    public RESPONSE call(REQUEST request) {
        try {
            MethodAndArgs methodAndArgs = codec.decodeRequest(request);
            Object ret = methodAndArgs.getMethod().invoke(implement, methodAndArgs.getArgs());
            return codec.encodeResponse(ret);
        } catch (Throwable e) {
            return errorHandler.onError(e);
        }
    }
}
