package net.goldolphin.maria.api.protoson;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.junit.Assert;
import org.junit.Test;

import com.google.protobuf.DoubleValue;
import com.google.protobuf.Int64Value;
import com.google.protobuf.Message;
import com.google.protobuf.StringValue;

import net.goldolphin.maria.HttpClient;
import net.goldolphin.maria.HttpContext;
import net.goldolphin.maria.HttpDispatcher;
import net.goldolphin.maria.HttpServer;
import net.goldolphin.maria.api.cli.CliEvaluator;
import net.goldolphin.maria.common.ExceptionUtils;

/**
 * Created by caofuxiang on 2017/4/20.
 */
public class ProtosonTest {
    private final Protoson<ApiClient> protoson = Protoson.create(ApiClient.class, new MyErrorCodec());

    @Test
    public void test() throws Exception {
        // Server
        HttpDispatcher dispatcher = new HttpDispatcher();
        dispatcher.registerController("/api/$", protoson.createHttpController(new Implement()));
        HttpServer httpServer = new HttpServer(new InetSocketAddress(6061), dispatcher);
        HttpClient httpClient = new HttpClient();
        try {
            httpServer.start();
            ApiClient apiClient = protoson.createClient("http://localhost:6061/api", httpClient, Duration.ofSeconds(10));

            Assert.assertEquals(10, apiClient.get10().get().getValue());
            Assert.assertEquals(Math.sin(100), apiClient.sin(DoubleValue.newBuilder().setValue(100).build()).get().getValue(), 0);
        } finally {
            httpClient.close(true);
            httpServer.stop(true);
        }
    }

    @Test
    public void testCli() throws IOException {
        CliEvaluator evaluator = protoson.createLocalCliEvaluator(new Implement(), "testCli 0.0.1");
        StringBuilder builder = new StringBuilder();
        builder.append("/help\n");
        builder.append("/help get10\n");
        builder.append("/help sin\n");
        builder.append("get10\n");
        builder.append("sin 10\n");
        builder.append("sinSync 10\n");
        builder.append("cos 10\n");
        builder.append("sin abc\n");
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

        DoubleValue sinSync(DoubleValue value);
    }

    public static class Implement {
        public Int64Value get10() {
            return Int64Value.newBuilder().setValue(10).build();
        }

        public Int64Value get10(HttpContext context) {
            return get10();
        }

        public CompletableFuture<DoubleValue> sin(DoubleValue value) {
            return CompletableFuture.completedFuture(DoubleValue.newBuilder().setValue(Math.sin(value.getValue())).build());
        }

        public CompletableFuture<DoubleValue> sin(DoubleValue value, HttpContext context) {
            return sin(value);
        }

        public CompletableFuture<DoubleValue> sinSync(DoubleValue value) {
            return sin(value);
        }

        public CompletableFuture<DoubleValue> sinSync(DoubleValue value, HttpContext context) {
            return sinSync(value);
        }
    }

    private static class MyErrorCodec implements ErrorCodec {

        @Override
        public Message encode(Throwable error) {
            return StringValue.newBuilder().setValue(ExceptionUtils.getRootCause(error).getMessage()).build();
        }

        @Override
        public Throwable decode(Message encoded) {
            return null;
        }

        @Override
        public Message getErrorMessageProtoType() {
            return StringValue.getDefaultInstance();
        }
    }
}
