package net.goldolphin.maria.api.protoson;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
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
import net.goldolphin.maria.api.cli.CliEvaluator;

/**
 * Created by caofuxiang on 2017/4/20.
 */
public class ProtosonTest {
    @Test
    public void test() throws NoSuchMethodException, ExecutionException, InterruptedException {
        // Server
        HttpDispatcher dispatcher = new HttpDispatcher();
        dispatcher.registerController("/api/$", Protoson.createHttpController(ApiClient.class, new Implement(),
                throwable -> {
                    throwable.printStackTrace();
                    return null;
                }, new ResponseVisitor() {
                    @Override
                    public Message getResult(Message response) {
                        return response;
                    }

                    @Override
                    public Message getError(Message response) {
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

    @Test
    public void testCli() throws NoSuchMethodException, IOException {
        CliEvaluator evaluator = Protoson.createLocalCliEvaluator(ApiClient.class, new Implement(),
                throwable -> {
                    throwable.printStackTrace();
                    return null;
                }, new ResponseVisitor() {
                    @Override
                    public Message getResult(Message response) {
                        return response;
                    }

                    @Override
                    public Message getError(Message response) {
                        return null;
                    }
                }, "testCli 0.0.1");

        StringBuilder builder = new StringBuilder();
        builder.append("/help\n");
        builder.append("/help get10\n");
        builder.append("/help sin\n");
        builder.append("get10\n");
        builder.append("sin 10\n");
        Reader input = new StringReader(builder.toString());
        Writer output = new StringWriter();
        Writer error = new StringWriter();
        evaluator.evaluate(input, output, error);
        System.out.println("\n--- Output:\n");
        System.out.println(output);
        System.out.println("\n--- Error:\n");
        System.out.println(error);
    }

    private interface ApiClient {
        CompletableFuture<Int64Value> get10();
        CompletableFuture<DoubleValue> sin(DoubleValue value);
    }

    public static class Implement {
        public Int64Value get10() {
            return Int64Value.newBuilder().setValue(10).build();
        }

        public CompletableFuture<DoubleValue> sin(DoubleValue value) {
            return CompletableFuture.completedFuture(DoubleValue.newBuilder().setValue(Math.sin(value.getValue())).build());
        }
    }
}
