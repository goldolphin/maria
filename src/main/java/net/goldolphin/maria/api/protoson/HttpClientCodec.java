package net.goldolphin.maria.api.protoson;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.Message;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.CharsetUtil;
import net.goldolphin.maria.api.ApiClientCodec;
import net.goldolphin.maria.api.reflect.MethodAndArgs;
import net.goldolphin.maria.api.reflect.ResultOrError;
import net.goldolphin.maria.common.ExceptionUtils;
import net.goldolphin.maria.common.JsonUtils;
import net.goldolphin.maria.common.MessageUtils;
import net.goldolphin.maria.common.UrlUtils;
import net.goldolphin.maria.serializer.ProtoSerializer;

/**
 * Created by caofuxiang on 2017/4/19.
 */
public class HttpClientCodec implements ApiClientCodec<MethodAndArgs, ResultOrError, HttpRequest, FullHttpResponse> {
    private final String serviceBase;
    private final Map<String, Entry> map;
    private final ErrorCodec errorCodec;
    private final ProtoSerializer protoSerializer;

    private HttpClientCodec(String serviceBase, Map<String, Entry> map, ErrorCodec errorCodec, ProtoSerializer protoSerializer) {
        this.serviceBase = serviceBase;
        this.map = map;
        this.errorCodec = errorCodec;
        this.protoSerializer = protoSerializer;
    }

    @Override
    public HttpRequest encodeRequest(MethodAndArgs request) {
        Object[] args = request.getArgs();
        try {
            return encodeRequest(serviceBase,
                                 request.getMethod().getName(),
                                 (args == null || args.length == 0) ? null : protoSerializer.toString((Message) args[0]));
        } catch (Exception e) {
            throw ExceptionUtils.toUnchecked(e);
        }
    }

    @Override
    public ResultOrError decodeResponse(MethodAndArgs request, FullHttpResponse encoded) {
        try {
            JsonNode json = JsonUtils.read(encoded.content().toString(CharsetUtil.UTF_8), JsonNode.class);
            if (json.has("error")) {
                Message error = protoSerializer.fromString(json.path("error").toString(),
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
                return ResultOrError.fromResult(protoSerializer.fromString(json.path("result").toString(),
                                                                           entry.responsePrototype.newBuilderForType()).build());
            }
        } catch (Exception e) {
            throw ExceptionUtils.toUnchecked(e);
        }
    }

    public static HttpRequest encodeRequest(String serviceBase, String method, String content) {
        String path = UrlUtils.concat(serviceBase, method);
        HttpRequest request = content == null
                ? MessageUtils.newHttpRequest(HttpMethod.POST, path)
                : MessageUtils.newHttpRequest(HttpMethod.POST, path, content);
        request.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/json;charset=UTF-8;");
        return request;
    }

    public static HttpClientCodec create(String serviceBase,
                                         Class<?> interfaceClass,
                                         ErrorCodec errorCodec,
                                         ProtoSerializer protoSerializer) {
        Map<String, Entry> map = new HashMap<>();
        for (Method method: (Iterable<Method>) ProtosonUtils.readInterface(interfaceClass)::iterator) {
            if (map.putIfAbsent(method.getName(), new Entry(ProtosonUtils.getResponsePrototype(method))) != null) {
                throw new IllegalArgumentException("Duplicate method name: " + method.getName());
            }
        }
        return new HttpClientCodec(serviceBase, map, errorCodec, protoSerializer);
    }

    private static class Entry {
        private final Message responsePrototype;

        private Entry(Message responsePrototype) {
            this.responsePrototype = responsePrototype;
        }
    }
}
