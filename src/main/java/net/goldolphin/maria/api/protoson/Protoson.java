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
import java.util.stream.Stream;

import com.google.protobuf.Message;

import net.goldolphin.maria.HttpClient;
import net.goldolphin.maria.IHttpController;
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
    public static <T> T createClient(Class<T> interfaceClass, ErrorCodec errorCodec,
            String serviceBase, HttpClient httpClient, long timeout, TimeUnit unit) {
        ReflectClientCodec codec = ReflectClientCodec.create(interfaceClass, errorCodec);
        HttpApiClientHandler<Request, Response> httpApiClientHandler
                = new HttpApiClientHandler<>(new HttpClientCodec(serviceBase), httpClient, timeout, unit);
        return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class<?>[] {interfaceClass}, (proxy, method, args) -> {
            MethodAndArgs methodAndArgs = new MethodAndArgs(method, args);
            CompletableFuture<Response> response = httpApiClientHandler.call(codec.encodeRequest(methodAndArgs));
            if (ProtosonUtils.isAsync(method)) {
                return response.thenApply(r -> {
                    ResultOrError resultOrError = codec.decodeResponse(methodAndArgs, r);
                    if (resultOrError.isError()) {
                        throw ExceptionUtils.toUnchecked(resultOrError.getError());
                    }
                    return resultOrError.getResult();
                });
            }
            ResultOrError resultOrError = codec.decodeResponse(methodAndArgs, response.get());
            if (resultOrError.isError()) {
                throw resultOrError.getError();
            }
            return resultOrError.getResult();

        });
    }

    public static IHttpController createHttpController(Class<?> interfaceClass, Object implement, ErrorCodec errorCodec) {
        ReflectServerCodec codec = ReflectServerCodec.create(interfaceClass, implement, errorCodec);
        return new HttpApiController<>(request -> {
            try {
                MethodAndArgs methodAndArgs = codec.decodeRequest(request);
                Object ret = methodAndArgs.getMethod().invoke(implement, methodAndArgs.getArgs());
                if (ret instanceof CompletableFuture<?>) {
                    return ((CompletableFuture<?>) ret).thenApply(o -> codec.encodeResponse(ResultOrError.fromResult(o)))
                            .exceptionally(throwable -> codec.encodeResponse(ResultOrError.fromError(throwable)));
                }
                return CompletableFuture.completedFuture(codec.encodeResponse(ResultOrError.fromResult(ret)));
            } catch (Throwable e) {
                return CompletableFuture.completedFuture(codec.encodeResponse(ResultOrError.fromError(e)));
            }
        }, new HttpServerCodec());
    }

    public static CliEvaluator createHttpCliEvaluator(Class<?> interfaceClass, String serviceBase, HttpClient httpClient, long timeout, TimeUnit unit,
            String description) {
        HttpApiClientHandler<Request, Response> httpClientHandler = new HttpApiClientHandler<>(new HttpClientCodec(serviceBase),
                httpClient, timeout, unit);
        return new CliEvaluator(request -> {
            try {
                return httpClientHandler.call(request).get();
            } catch (Throwable e) {
                throw ExceptionUtils.toUnchecked(e);
            }
        }, new CliCodec(), createCliCommandHandler(interfaceClass), description);
    }

    public static CliEvaluator createLocalCliEvaluator(Class<?> interfaceClass, Object implement, ErrorCodec errorCodec, String description) {
        ReflectServerCodec codec = ReflectServerCodec.create(interfaceClass, implement, errorCodec);
        return new CliEvaluator(request -> {
            try {
                MethodAndArgs methodAndArgs = codec.decodeRequest(request);
                Object ret = methodAndArgs.getMethod().invoke(implement, methodAndArgs.getArgs());
                if (ret instanceof CompletableFuture<?>) {
                    return codec.encodeResponse(ResultOrError.fromResult(((CompletableFuture<?>) ret).get()));
                }
                return codec.encodeResponse(ResultOrError.fromResult(ret));
            } catch (Throwable e) {
                return codec.encodeResponse(ResultOrError.fromError(e));
            }
        }, new CliCodec(), createCliCommandHandler(interfaceClass), description);
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
