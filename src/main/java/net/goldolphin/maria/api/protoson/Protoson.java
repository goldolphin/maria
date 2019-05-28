package net.goldolphin.maria.api.protoson;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import net.goldolphin.maria.HttpClient;
import net.goldolphin.maria.IHttpController;
import net.goldolphin.maria.api.ApiHandler;
import net.goldolphin.maria.api.cli.CliCommandHandler;
import net.goldolphin.maria.api.cli.CliEvaluator;
import net.goldolphin.maria.api.http.HttpApiClientHandler;
import net.goldolphin.maria.api.http.HttpApiController;
import net.goldolphin.maria.api.reflect.MethodAndArgs;
import net.goldolphin.maria.api.reflect.ResultOrError;
import net.goldolphin.maria.common.ExceptionUtils;
import net.goldolphin.maria.serializer.ProtoJsonSerializer;
import net.goldolphin.maria.serializer.ProtoSerializer;

/**
 * Created by caofuxiang on 2017/4/20.
 */
public class Protoson<T> {
    private static final ProtoSerializer DEFAULT_PROTO_SERIALIZER
            = new ProtoJsonSerializer(JsonFormat.printer().includingDefaultValueFields().omittingInsignificantWhitespace(),
                                      JsonFormat.parser().ignoringUnknownFields());

    private final Class<T> interfaceClass;
    private final ErrorCodec errorCodec;
    private final ProtoSerializer protoSerializer;

    public static <T> Protoson<T> create(Class<T> interfaceClass, ErrorCodec errorCodec, ProtoSerializer protoSerializer) {
        return new Protoson<>(interfaceClass, errorCodec, protoSerializer);
    }

    public static <T> Protoson<T> create(Class<T> interfaceClass, ErrorCodec errorCodec) {
        return new Protoson<>(interfaceClass, errorCodec, DEFAULT_PROTO_SERIALIZER);
    }

    private Protoson(Class<T> interfaceClass, ErrorCodec errorCodec, ProtoSerializer protoSerializer) {
        this.interfaceClass = interfaceClass;
        this.errorCodec = errorCodec;
        this.protoSerializer = protoSerializer;
    }

    public T createClient(String serviceBase, HttpClient httpClient, Duration timeout) {
        return createClient(createHttpClientHandler(serviceBase, new HttpApiClientHandler(httpClient, timeout)));
    }

    @SuppressWarnings("unchecked")
    public T createClient(ApiHandler<MethodAndArgs, CompletableFuture<ResultOrError>> clientHandler) {
        return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class<?>[] {interfaceClass}, (proxy, method, args) -> {
            MethodAndArgs methodAndArgs = new MethodAndArgs(method, args);
            if (ProtosonUtils.isAsync(method)) {
                return clientHandler.call(methodAndArgs).thenApply(resultOrError -> {
                    if (resultOrError.isError()) {
                        throw ExceptionUtils.toUnchecked(resultOrError.getError());
                    }
                    return resultOrError.getResult();
                });
            } else {
                ResultOrError resultOrError = clientHandler.call(methodAndArgs).get();
                if (resultOrError.isError()) {
                    throw resultOrError.getError();
                }
                return resultOrError.getResult();
            }
        });
    }

    public ApiHandler<MethodAndArgs, CompletableFuture<ResultOrError>> createHttpClientHandler(
            String serviceBase,
            ApiHandler<HttpRequest, CompletableFuture<FullHttpResponse>> httpHandler) {
        HttpClientCodec codec = HttpClientCodec.create(serviceBase, interfaceClass, errorCodec, protoSerializer);
        return request -> httpHandler.call(codec.encodeRequest(request)).thenApply(r -> codec.decodeResponse(request, r));
    }

    public static ApiHandler<MethodAndArgs, CompletableFuture<ResultOrError>> createReflectHandler(Object implement) {
        return methodAndArgs -> {
            try {
                Object ret = methodAndArgs.getMethod().invoke(implement, methodAndArgs.getArgs());
                if (ret instanceof CompletableFuture<?>) {
                    return ((CompletableFuture<?>) ret).thenApply(ResultOrError::fromResult)
                            .exceptionally(ResultOrError::fromError);
                }
                return CompletableFuture.completedFuture(ResultOrError.fromResult(ret));
            } catch (Exception e) {
                return CompletableFuture.completedFuture(ResultOrError.fromError(e));
            }
        };
    }

    public IHttpController createHttpController(Object implement) {
        return createHttpController(implement.getClass(), createReflectHandler(implement));
    }

    public IHttpController createHttpController(Class<?> implementClass,
            ApiHandler<MethodAndArgs, CompletableFuture<ResultOrError>> handler) {
        HttpServerCodec codec = HttpServerCodec.create(interfaceClass, implementClass, errorCodec, protoSerializer);
        return new HttpApiController(context -> {
            try {
                return handler.call(codec.decodeRequest(context)).thenApply(codec::encodeResponse);
            } catch (Exception e) {
                return CompletableFuture.completedFuture(codec.encodeResponse(ResultOrError.fromError(e)));
            }
        });
    }

    public CliEvaluator createHttpCliEvaluator(String serviceBase, HttpClient httpClient, Duration timeout, String description) {
        return createHttpCliEvaluator(createHttpClientHandler(serviceBase, httpClient, timeout), description);
    }

    public CliEvaluator createHttpCliEvaluator(ApiHandler<String[], CompletableFuture<String>> clientHandler, String description) {
        return new CliEvaluator(request -> {
            try {
                return clientHandler.call(request).get();
            } catch (Exception e) {
                throw ExceptionUtils.toUnchecked(e);
            }
        }, createCliCommandHandler(interfaceClass), description);
    }

    public static ApiHandler<String[], CompletableFuture<String>> createHttpClientHandler(
            String serviceBase, HttpClient httpClient, Duration timeout) {
        HttpCliCodec codec = HttpCliCodec.create(serviceBase);
        HttpApiClientHandler handler = new HttpApiClientHandler(httpClient, timeout);
        return request -> handler.call(codec.decodeRequest(request)).thenApply(codec::encodeResponse);
    }

    public CliEvaluator createLocalCliEvaluator(Object implement, String description) {
        return createLocalCliEvaluator(implement.getClass(), description, createReflectHandler(implement));
    }

    public CliEvaluator createLocalCliEvaluator(Class<?> implementClass,
                                                String description,
                                                ApiHandler<MethodAndArgs, CompletableFuture<ResultOrError>> handler) {
        ReflectCliCodec codec = ReflectCliCodec.create(interfaceClass, implementClass, errorCodec, protoSerializer);
        return new CliEvaluator(r -> {
            try {
                return codec.encodeResponse(handler.call(codec.decodeRequest(r)).get());
            } catch (Exception e) {
                return codec.encodeResponse(ResultOrError.fromError(e));
            }
        }, createCliCommandHandler(interfaceClass), description);
    }

    public static CliCommandHandler createCliCommandHandler(Class<?> interfaceClass) {
        List<String> methods = new ArrayList<>();
        Map<String, String> methodHelpMap = new HashMap<>();
        for (Method method: (Iterable<Method>) readInterface(interfaceClass)::iterator) {
            methods.add(method.getName());
            Message requestPrototype = ProtosonUtils.getRequestPrototype(method);
            Message responsePrototype = ProtosonUtils.getResponsePrototype(method);
            StringBuilder builder = new StringBuilder();
            builder.append("Argument: ").append(ProtosonUtils.buildSchemaString(requestPrototype));
            builder.append("\nResult: ").append(ProtosonUtils.buildSchemaString(responsePrototype));
            methodHelpMap.put(method.getName(), builder.toString());
        }
        return new CliCommandHandler() {
            @Override
            public List<String> list() {
                return methods;
            }

            @Override
            public String help(String method) {
                return methodHelpMap.get(method);
            }
        };
    }

    private static Stream<Method> readInterface(Class<?> interfaceClass) {
        return ProtosonUtils.readInterface(interfaceClass).sorted(Comparator.comparing(m -> new StringBuilder(m.getName()).reverse().toString()));
    }
}
