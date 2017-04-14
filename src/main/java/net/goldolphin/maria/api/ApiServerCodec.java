package net.goldolphin.maria.api;

import java.lang.reflect.Method;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponse;

/**
 * Created by caofuxiang on 2017/4/14.
 */
public interface ApiServerCodec {
    DecodeResult decodeRequest(FullHttpRequest httpRequest);
    HttpResponse encodeResponse(Object response);

    /**
     * Decode result.
     */
    class DecodeResult {
        private final Method method;
        private final Object[] args;
        private final Object errorResponse;

        public DecodeResult(Method method, Object[] args, Object errorResponse) {
            this.method = method;
            this.args = args;
            this.errorResponse = errorResponse;
        }

        public Method getMethod() {
            return method;
        }

        public Object[] getArgs() {
            return args;
        }

        public Object getErrorResponse() {
            return errorResponse;
        }
    }
}
