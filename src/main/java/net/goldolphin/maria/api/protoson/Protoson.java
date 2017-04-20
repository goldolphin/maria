package net.goldolphin.maria.api.protoson;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import net.goldolphin.maria.HttpClient;
import net.goldolphin.maria.IHttpController;
import net.goldolphin.maria.api.ApiClientCodec;
import net.goldolphin.maria.api.ApiServerCodec;
import net.goldolphin.maria.api.http.HttpApiClientHandler;
import net.goldolphin.maria.api.http.HttpApiController;
import net.goldolphin.maria.api.reflect.MethodAndArgs;
import net.goldolphin.maria.api.reflect.ReflectApiClients;
import net.goldolphin.maria.api.reflect.ReflectApiServerHandler;

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

    public static IHttpController createHttpController(Class<?> interfaceClass, Object implement, ErrorHandler errorHandler)
            throws NoSuchMethodException {
        ReflectServerCodec codec = ReflectServerCodec.create(interfaceClass, implement, errorHandler);
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
                            return ((CompletableFuture<?>) o).thenApply(codec::encodeResponse);
                        }
                        return CompletableFuture.completedFuture(codec.encodeResponse(o));
                    }
                }, errorHandler),
                new HttpServerCodec());
    }
}
