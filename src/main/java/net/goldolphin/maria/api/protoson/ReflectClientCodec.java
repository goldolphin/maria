package net.goldolphin.maria.api.protoson;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.Message;

import net.goldolphin.maria.api.ApiClientCodec;
import net.goldolphin.maria.api.reflect.MethodAndArgs;
import net.goldolphin.maria.api.reflect.ResultOrError;
import net.goldolphin.maria.common.ExceptionUtils;
import net.goldolphin.maria.common.JsonUtils;
import net.goldolphin.maria.common.ProtoJsonCodec;

/**
 * Created by caofuxiang on 2017/4/19.
 */
public class ReflectClientCodec implements ApiClientCodec<MethodAndArgs, ResultOrError, Request, Response> {
    private final Map<String, Entry> map;
    private final ErrorCodec errorCodec;

    private ReflectClientCodec(Map<String, Entry> map, ErrorCodec errorCodec) {
        this.map = map;
        this.errorCodec = errorCodec;
    }

    @Override
    public Request encodeRequest(MethodAndArgs request) {
        Object[] args = request.getArgs();
        if (args == null || args.length == 0) {
            return new Request(request.getMethod().getName(), null);
        }
        return new Request(request.getMethod().getName(), ProtoJsonCodec.toString((Message) args[0]));
    }

    @Override
    public ResultOrError decodeResponse(MethodAndArgs request, Response encoded) {
        try {
            JsonNode json = JsonUtils.read(encoded.getContent(), JsonNode.class);
            if (json.has("error")) {
                Message error = ProtoJsonCodec.fromString(json.path("error").toString(),
                        errorCodec.getErrorMessageProtoType().newBuilderForType()).build();
                return ResultOrError.fromError(errorCodec.decode(error));
            }
            Entry entry = map.get(request.getMethod().getName());
            if (entry == null) {
                throw new NoSuchMethodException(request.getMethod().getName());
            }
            if (entry.responsePrototype == null) {
                return ResultOrError.fromResult(null);
            } else {
                return ResultOrError.fromResult(ProtoJsonCodec.fromString(json.path("result").toString(),
                        entry.responsePrototype.newBuilderForType()).build());
            }
        } catch (Throwable e) {
            throw ExceptionUtils.toUnchecked(e);
        }
    }

    public static ReflectClientCodec create(Class<?> interfaceClass, ErrorCodec errorCodec) {
        Map<String, Entry> map = new HashMap<>();
        for (Method method: (Iterable<Method>) ProtosonUtils.readInterface(interfaceClass)::iterator) {
            if (map.putIfAbsent(method.getName(), new Entry(ProtosonUtils.getResponsePrototype(method))) != null) {
                throw new IllegalArgumentException("Duplicate method name: " + method.getName());
            }
        }
        return new ReflectClientCodec(map, errorCodec);
    }

    private static class Entry {
        private final Message responsePrototype;

        private Entry(Message responsePrototype) {
            this.responsePrototype = responsePrototype;
        }
    }
}
