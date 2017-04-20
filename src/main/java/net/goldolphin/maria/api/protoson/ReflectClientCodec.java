package net.goldolphin.maria.api.protoson;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.Message;

import net.goldolphin.maria.api.ApiClientCodec;
import net.goldolphin.maria.api.reflect.MethodAndArgs;
import net.goldolphin.maria.common.ExceptionUtils;
import net.goldolphin.maria.common.JsonUtils;
import net.goldolphin.maria.common.ProtoJsonCodec;

/**
 * Created by caofuxiang on 2017/4/19.
 */
public class ReflectClientCodec implements ApiClientCodec<MethodAndArgs, Object, Request, Response> {
    private final Map<String, Entry> map;

    private ReflectClientCodec(Map<String, Entry> map) {
        this.map = map;
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
    public Object decodeResponse(MethodAndArgs request, Response encoded) {
        try {
            JsonNode json = JsonUtils.read(encoded.getContent(), JsonNode.class);
            if (json.has("error")) {
                throw new IOException(json.path("description").asText());
            }
            Entry entry = map.get(request.getMethod().getName());
            if (entry == null) {
                throw new NoSuchMethodException(request.getMethod().getName());
            }
            if (entry.responsePrototype == null) {
                return null;
            } else {
                return ProtoJsonCodec.fromString(json.path("result").toString(), entry.responsePrototype.newBuilderForType()).build();
            }
        } catch (Throwable e) {
            throw ExceptionUtils.toUnchecked(e);
        }
    }

    public static ReflectClientCodec create(Class<?> interfaceClass) {
        Map<String, Entry> map = new HashMap<>();
        for (Method method: (Iterable<Method>) ProtosonUtils.readInterface(interfaceClass)::iterator) {
            map.put(method.getName(), new Entry(ProtosonUtils.getResponsePrototype(method)));
        }
        return new ReflectClientCodec(map);
    }

    private static class Entry {
        private final Message responsePrototype;

        private Entry(Message responsePrototype) {
            this.responsePrototype = responsePrototype;
        }
    }
}
