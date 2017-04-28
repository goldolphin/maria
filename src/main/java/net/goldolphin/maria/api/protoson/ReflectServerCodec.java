package net.goldolphin.maria.api.protoson;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;

import net.goldolphin.maria.api.ApiServerCodec;
import net.goldolphin.maria.api.reflect.MethodAndArgs;
import net.goldolphin.maria.api.reflect.ResultOrError;
import net.goldolphin.maria.common.ExceptionUtils;
import net.goldolphin.maria.common.JsonUtils;
import net.goldolphin.maria.common.ProtoJsonCodec;

/**
 * Created by caofuxiang on 2017/4/19.
 */
public class ReflectServerCodec implements ApiServerCodec<MethodAndArgs, ResultOrError, Request, Response> {
    private final Map<String, Entry> map;
    private final ErrorCodec errorCodec;

    private ReflectServerCodec(Map<String, Entry> map, ErrorCodec errorCodec) {
        this.map = map;
        this.errorCodec = errorCodec;
    }

    @Override
    public MethodAndArgs decodeRequest(Request encoded) {
        Entry entry = map.get(encoded.getMethod());
        try {
            if (entry == null) {
                throw new NoSuchMethodException(encoded.getMethod());
            }
            if (entry.requestPrototype == null) {
                return new MethodAndArgs(entry.method, new Object[0]);
            } else {
                return new MethodAndArgs(entry.method, new Object[]{
                        ProtoJsonCodec.fromString(encoded.getContent(),
                                entry.requestPrototype.newBuilderForType()).build()});
            }
        } catch (Throwable e) {
            throw ExceptionUtils.toUnchecked(e);
        }
    }

    @Override
    public Response encodeResponse(ResultOrError resultOrError) {
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
            return new Response(writer.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ReflectServerCodec create(Class<?> interfaceClass, Object implement, ErrorCodec errorCodec) {
        Map<String, Entry> map = new HashMap<>();
        for (Method m: (Iterable<Method>) ProtosonUtils.readInterface(interfaceClass)::iterator) {
            try {
                Method method = implement.getClass().getMethod(m.getName(), m.getParameterTypes());
                if (map.putIfAbsent(method.getName(), new Entry(method, ProtosonUtils.getRequestPrototype(method))) != null){
                    throw new IllegalArgumentException("Duplicate method name: " + method.getName());
                }
            } catch (Exception e) {
                throw ExceptionUtils.toUnchecked(e);
            }
        }
        return new ReflectServerCodec(map, errorCodec);
    }

    private static class Entry {
        private final Method method;
        private final Message requestPrototype;

        public Entry(Method method, Message requestPrototype) {
            this.method = method;
            this.requestPrototype = requestPrototype;
        }
    }
}
