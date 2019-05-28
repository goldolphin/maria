package net.goldolphin.maria.api.protoson;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.Message;

import net.goldolphin.maria.api.ApiServerCodec;
import net.goldolphin.maria.api.reflect.MethodAndArgs;
import net.goldolphin.maria.api.reflect.ResultOrError;
import net.goldolphin.maria.common.ExceptionUtils;
import net.goldolphin.maria.serializer.ProtoSerializer;

/**
 * Created by caofuxiang on 2017/4/21.
 */
public class ReflectCliCodec implements ApiServerCodec<MethodAndArgs, ResultOrError, String[], String> {
    private final Map<String, Entry> map;
    private final ErrorCodec errorCodec;
    private final ProtoSerializer protoSerializer;

    private ReflectCliCodec(Map<String, Entry> map, ErrorCodec errorCodec, ProtoSerializer protoSerializer) {
        this.map = map;
        this.errorCodec = errorCodec;
        this.protoSerializer = protoSerializer;
    }

    @Override
    public MethodAndArgs decodeRequest(String[] encoded) {
        String method;
        String content;
        if (encoded.length >= 2) {
            method = encoded[0];
            content = encoded[1];
        } else {
            method = encoded[0];
            content = null;
        }
        Entry entry = map.get(method);
        try {
            if (entry == null) {
                throw new NoSuchMethodException(method);
            }
            if (entry.requestPrototype == null) {
                return new MethodAndArgs(entry.method);
            } else {
                return new MethodAndArgs(entry.method, protoSerializer.fromString(content, entry.requestPrototype.newBuilderForType()).build());
            }
        } catch (Exception e) {
            throw ExceptionUtils.toUnchecked(e);
        }
    }

    @Override
    public String encodeResponse(ResultOrError resultOrError) {
        return HttpServerCodec.encodeResponse(errorCodec, resultOrError, protoSerializer);
    }

    public static ReflectCliCodec create(Class<?> interfaceClass,
                                         Class<?> implementClass, ErrorCodec errorCodec,
                                         ProtoSerializer protoSerializer) {
        Map<String, Entry> map = new HashMap<>();
        for (Method method: (Iterable<Method>) ProtosonUtils.readInterface(interfaceClass)::iterator) {
            try {
                Message requestPrototype = ProtosonUtils.getRequestPrototype(method);
                Method implementMethod = requestPrototype == null
                        ? implementClass.getMethod(method.getName())
                        : implementClass.getMethod(method.getName(), requestPrototype.getClass());
                if (map.putIfAbsent(method.getName(), new Entry(implementMethod, requestPrototype)) != null) {
                    throw new IllegalArgumentException("Duplicate method name: " + method.getName());
                }
            } catch (Exception e) {
                throw ExceptionUtils.toUnchecked(e);
            }
        }
        return new ReflectCliCodec(map, errorCodec, protoSerializer);
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
