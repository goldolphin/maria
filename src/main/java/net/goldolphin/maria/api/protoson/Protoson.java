package net.goldolphin.maria.api.protoson;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.protobuf.Message;

import io.netty.handler.codec.http.FullHttpRequest;
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

/**
 * Created by caofuxiang on 2017/4/20.
 */
public class Protoson {
    public static <T> T createClient(Class<?> interfaceClass, ErrorCodec errorCodec, String serviceBase,
            HttpClient httpClient, long timeout, TimeUnit unit) {
        return createClient(
                interfaceClass,
                createHttpClientHandler(interfaceClass, errorCodec, serviceBase, new HttpApiClientHandler(httpClient, timeout, unit)));
    }

    public static <T> T createClient(Class<?> interfaceClass, ApiHandler<MethodAndArgs, CompletableFuture<ResultOrError>> clientHandler) {
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

    public static ApiHandler<MethodAndArgs, CompletableFuture<ResultOrError>> createHttpClientHandler(
            Class<?> interfaceClass, ErrorCodec errorCodec, String serviceBase,
            ApiHandler<HttpRequest, CompletableFuture<FullHttpResponse>> httpHandler) {
        ReflectClientCodec codec1 = ReflectClientCodec.create(interfaceClass, errorCodec);
        HttpClientCodec codec2 = new HttpClientCodec(serviceBase);
        return request1 -> {
            Request request2 = codec1.encodeRequest(request1);
            return httpHandler.call(codec2.encodeRequest(request2))
                    .thenApply(r -> codec1.decodeResponse(request1, codec2.decodeResponse(request2, r)));
        };
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
            } catch (Throwable e) {
                return CompletableFuture.completedFuture(ResultOrError.fromError(e));
            }
        };
    }

    public static IHttpController createHttpController(Class<?> interfaceClass, Object implement, ErrorCodec errorCodec) {
        ApiHandler<MethodAndArgs, CompletableFuture<ResultOrError>> handler = createReflectHandler(implement);
        return createHttpController(interfaceClass, implement.getClass(), errorCodec, r -> handler);
    }

    public static IHttpController createHttpController(Class<?> interfaceClass, Class<?> implementClass, ErrorCodec errorCodec,
            Function<FullHttpRequest, ApiHandler<MethodAndArgs, CompletableFuture<ResultOrError>>> handlerFactory) {
        ReflectServerCodec codec1 = ReflectServerCodec.create(interfaceClass, implementClass, errorCodec);
        HttpServerCodec codec2 = new HttpServerCodec();
        return new HttpApiController(r -> {
            try {
                MethodAndArgs methodAndArgs = codec1.decodeRequest(codec2.decodeRequest(r));
                return handlerFactory.apply(r).call(methodAndArgs)
                        .thenApply(resultOrError -> codec2.encodeResponse(codec1.encodeResponse(resultOrError)));
            } catch (Throwable e) {
                return CompletableFuture.completedFuture(codec2.encodeResponse(codec1.encodeResponse(ResultOrError.fromError(e))));
            }
        });
    }

    public static CliEvaluator createHttpCliEvaluator(Class<?> interfaceClass, String serviceBase, HttpClient httpClient, long timeout, TimeUnit unit,
            String description) {
        return createHttpCliEvaluator(interfaceClass, createHttpClientHandler(serviceBase, httpClient, timeout, unit), description);
    }

    public static CliEvaluator createHttpCliEvaluator(Class<?> interfaceClass, ApiHandler<String[], CompletableFuture<String>> clientHandler,
            String description) {
        return new CliEvaluator(request -> {
            try {
                return clientHandler.call(request).get();
            } catch (Throwable e) {
                throw ExceptionUtils.toUnchecked(e);
            }
        }, createCliCommandHandler(interfaceClass), description);
    }

    public static ApiHandler<String[], CompletableFuture<String>> createHttpClientHandler(
            String serviceBase, HttpClient httpClient, long timeout, TimeUnit unit) {
        CliCodec codec1 = new CliCodec();
        HttpClientCodec codec2 = new HttpClientCodec(serviceBase);
        HttpApiClientHandler handler = new HttpApiClientHandler(httpClient, timeout, unit);
        return request1 -> {
            Request request2 = codec1.decodeRequest(request1);
            return handler.call(codec2.encodeRequest(request2)).thenApply(r -> codec1.encodeResponse(codec2.decodeResponse(request2, r)));
        };
    }

    public static CliEvaluator createLocalCliEvaluator(Class<?> interfaceClass, Object implement, ErrorCodec errorCodec, String description) {
        return createLocalCliEvaluator(interfaceClass, implement.getClass(), errorCodec, description, createReflectHandler(implement));
    }

    public static CliEvaluator createLocalCliEvaluator(Class<?> interfaceClass, Class<?> implementClass, ErrorCodec errorCodec, String description,
            ApiHandler<MethodAndArgs, CompletableFuture<ResultOrError>> handler) {
        CliCodec codec1 = new CliCodec();
        ReflectServerCodec codec2 = ReflectServerCodec.create(interfaceClass, implementClass, errorCodec);
        return new CliEvaluator(r -> {
            try {
                MethodAndArgs methodAndArgs = codec2.decodeRequest(codec1.decodeRequest(r));
                return codec1.encodeResponse(codec2.encodeResponse(handler.call(methodAndArgs).get()));
            } catch (Throwable e) {
                return codec1.encodeResponse(codec2.encodeResponse(ResultOrError.fromError(e)));
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
