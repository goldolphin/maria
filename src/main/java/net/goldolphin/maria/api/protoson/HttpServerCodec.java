package net.goldolphin.maria.api.protoson;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;

import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;
import net.goldolphin.maria.HttpContext;
import net.goldolphin.maria.api.ApiServerCodec;
import net.goldolphin.maria.api.reflect.MethodAndArgs;
import net.goldolphin.maria.api.reflect.ResultOrError;
import net.goldolphin.maria.common.ExceptionUtils;
import net.goldolphin.maria.common.JsonUtils;
import net.goldolphin.maria.common.MessageUtils;
import net.goldolphin.maria.common.ProtoJsonCodec;
import net.goldolphin.maria.common.UrlUtils;

/**
 * Created by caofuxiang on 2017/4/19.
 */
public class HttpServerCodec implements ApiServerCodec<MethodAndArgs, ResultOrError, HttpContext, HttpResponse> {
    private final Map<String, Entry> map;
    private final ErrorCodec errorCodec;

    private HttpServerCodec(Map<String, Entry> map, ErrorCodec errorCodec) {
        this.map = map;
        this.errorCodec = errorCodec;
    }

    @Override
    public MethodAndArgs decodeRequest(HttpContext context) {
        URI uri = URI.create(context.getRequest().getUri());
        String method = UrlUtils.getBasename(uri.getPath());
        Entry entry = map.get(method);
        try {
            if (entry == null) {
                throw new NoSuchMethodException(method);
            }
            if (entry.requestPrototype == null) {
                return new MethodAndArgs(entry.method, context);
            } else {
                return new MethodAndArgs(entry.method,
                        ProtoJsonCodec.fromString(
                                context.getRequest().content().toString(CharsetUtil.UTF_8),
                                entry.requestPrototype.newBuilderForType()).build(),
                        context);
            }
        } catch (Throwable e) {
            throw ExceptionUtils.toUnchecked(e);
        }
    }

    @Override
    public HttpResponse encodeResponse(ResultOrError resultOrError) {
        return MessageUtils.newHttpResponse(HttpResponseStatus.OK, "text/json;charset=UTF-8;", encodeResponse(errorCodec, resultOrError));
    }

    public static String encodeResponse(ErrorCodec errorCodec, ResultOrError resultOrError) {
        StringWriter writer = new StringWriter();
        try {
            JsonGenerator generator = JsonUtils.factory().createGenerator(writer);
            generator.writeStartObject();
            if (resultOrError.isError()) {
                generator.writeFieldName("error");
                generator.writeRawValue(ProtoJsonCodec.toString(errorCodec.encode(resultOrError.getError())));
            } else {
                if (resultOrError.getResult() != null) {
                    generator.writeFieldName("result");
                    generator.writeRawValue(ProtoJsonCodec.toString((MessageOrBuilder) resultOrError.getResult()));
                }
            }
            generator.writeEndObject();
            generator.flush();
            return writer.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static HttpServerCodec create(Class<?> interfaceClass, Class<?> implementClass, ErrorCodec errorCodec) {
        Map<String, Entry> map = new HashMap<>();
        for (Method method: (Iterable<Method>) ProtosonUtils.readInterface(interfaceClass)::iterator) {
            try {
                Message requestPrototype = ProtosonUtils.getRequestPrototype(method);
                Method implementMethod = requestPrototype == null
                        ? implementClass.getMethod(method.getName(), HttpContext.class)
                        : implementClass.getMethod(method.getName(), requestPrototype.getClass(), HttpContext.class);
                if (map.putIfAbsent(method.getName(), new Entry(implementMethod, requestPrototype)) != null) {
                    throw new IllegalArgumentException("Duplicate method name: " + method.getName());
                }
            } catch (Exception e) {
                throw ExceptionUtils.toUnchecked(e);
            }
        }
        return new HttpServerCodec(map, errorCodec);
    }

    private static class Entry {
        private final Method method;
        private final Message requestPrototype;

        private Entry(Method method, Message requestPrototype) {
            this.method = method;
            this.requestPrototype = requestPrototype;
        }
    }
}
