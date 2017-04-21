package net.goldolphin.maria.api.protoson;

import java.lang.reflect.Method;
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
import net.goldolphin.maria.api.ApiClientCodec;
import net.goldolphin.maria.api.ApiServerCodec;
import net.goldolphin.maria.api.cli.CliCommandHandler;
import net.goldolphin.maria.api.cli.CliEvaluator;
import net.goldolphin.maria.api.http.HttpApiClientHandler;
import net.goldolphin.maria.api.http.HttpApiController;
import net.goldolphin.maria.api.reflect.ErrorHandler;
import net.goldolphin.maria.api.reflect.MethodAndArgs;
import net.goldolphin.maria.api.reflect.ReflectApiClients;
import net.goldolphin.maria.api.reflect.ReflectApiServerHandler;
import net.goldolphin.maria.common.ExceptionUtils;

/**
 * Created by caofuxiang on 2017/4/20.
 */
public class Protoson {
    public static <T> T createClient(Class<T> interfaceClass, String serviceBase, HttpClient httpClient, long timeout, TimeUnit unit) {
        ReflectClientCodec codec = ReflectClientCodec.create(interfaceClass);
        return ReflectApiClients.createClient(interfaceClass,
                new HttpApiClientHandler<>(new HttpClientCodec(serviceBase), httpClient, timeout, unit),
                new ApiClientCodec<MethodAndArgs, Object, Request, CompletableFuture<Response>>() {
                    @Override
                    public Request encodeRequest(MethodAndArgs methodAndArgs) {
                        return codec.encodeRequest(methodAndArgs);
                    }

                    @Override
                    public Object decodeResponse(MethodAndArgs methodAndArgs, CompletableFuture<Response> future) {
                        return future.thenApply(response -> codec.decodeResponse(methodAndArgs, response));
                    }
                }
        );
    }

    public static IHttpController createHttpController(Class<?> interfaceClass, Object implement, ErrorHandler<Message> errorHandler,
            ResponseVisitor responseVisitor) throws NoSuchMethodException {
        ReflectServerCodec codec = ReflectServerCodec.create(interfaceClass, implement, responseVisitor);
        return new HttpApiController<>(new ReflectApiServerHandler<>(
                implement,
                new ApiServerCodec<MethodAndArgs, Object, Request, CompletableFuture<Response>>() {
                    @Override
                    public MethodAndArgs decodeRequest(Request encoded) {
                        return codec.decodeRequest(encoded);
                    }

                    @Override
                    public CompletableFuture<Response> encodeResponse(Object o) {
                        if (o instanceof CompletableFuture<?>) {
                            return ((CompletableFuture<Object>) o).exceptionally(errorHandler::onError).thenApply(codec::encodeResponse);
                        }
                        return CompletableFuture.completedFuture(codec.encodeResponse(o));
                    }
                }, errorHandler),
                new HttpServerCodec());
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

    public static CliEvaluator createLocalCliEvaluator(Class<?> interfaceClass, Object implement, ErrorHandler<Message> errorHandler,
            ResponseVisitor responseVisitor, String description) throws NoSuchMethodException {
        ReflectServerCodec codec = ReflectServerCodec.create(interfaceClass, implement, responseVisitor);
        return new CliEvaluator(new ReflectApiServerHandler<>(implement,
                new ApiServerCodec<MethodAndArgs, Object, Request, Response>() {
                    @Override
                    public MethodAndArgs decodeRequest(Request encoded) {
                        return codec.decodeRequest(encoded);
                    }

                    @Override
                    public Response encodeResponse(Object o) {
                        if (o instanceof CompletableFuture<?>) {
                            try {
                                return codec.encodeResponse(((CompletableFuture<?>) o).get());
                            } catch (Exception e) {
                                throw ExceptionUtils.toUnchecked(e);
                            }
                        }
                        return codec.encodeResponse(o);
                    }
                }, errorHandler),
                new CliCodec(),
                createCliCommandHandler(interfaceClass), description);
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
