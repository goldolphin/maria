package net.goldolphin.maria.api.protoson;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import com.google.protobuf.DoubleValue;
import com.google.protobuf.Int64Value;
import com.google.protobuf.Message;

import net.goldolphin.maria.HttpClient;
import net.goldolphin.maria.HttpDispatcher;
import net.goldolphin.maria.HttpServer;

/**
 * Created by caofuxiang on 2017/4/20.
 */
public class ProtosonTest {
    @Test
    public void test() throws NoSuchMethodException, ExecutionException, InterruptedException {
        // Server
        HttpDispatcher dispatcher = new HttpDispatcher();
        dispatcher.registerController("/api/$", Protoson.createHttpController(ApiClient.class, new Implement(),
                new ErrorHandler() {
                    @Override
                    public Message getResult(Message response) {
                        return response;
                    }

                    @Override
                    public Message getError(Message response) {
                        return null;
                    }

                    @Override
                    public Message onError(Throwable throwable) {
                        return null;
                    }
                }));
        HttpServer httpServer = new HttpServer(new InetSocketAddress(6061), dispatcher);
        HttpClient httpClient = new HttpClient();
        try {
            httpServer.start();
            ApiClient apiClient = Protoson.createClient(ApiClient.class, "http://localhost:6061/api",
                    httpClient, 10, TimeUnit.SECONDS);

            Assert.assertEquals(10, apiClient.get10().get().getValue());
            Assert.assertEquals(Math.sin(100), apiClient.sin(DoubleValue.newBuilder().setValue(100).build()).get().getValue(), 0);
        } finally {
            httpServer.stop(true);
            httpClient.getWorkerGroup().shutdownGracefully();
        }
    }

    public interface ApiClient {
        CompletableFuture<Int64Value> get10();
        CompletableFuture<DoubleValue> sin(DoubleValue value);
    }

    public class Implement {
        public Int64Value get10() {
            return Int64Value.newBuilder().setValue(10).build();
        }

        public CompletableFuture<DoubleValue> sin(DoubleValue value) {
            return CompletableFuture.completedFuture(DoubleValue.newBuilder().setValue(Math.sin(value.getValue())).build());
        }
    }
}
