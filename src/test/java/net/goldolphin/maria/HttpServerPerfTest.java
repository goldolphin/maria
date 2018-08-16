package net.goldolphin.maria;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.RateLimiter;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Histogram;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.reporting.ConsoleReporter;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import net.goldolphin.maria.common.MessageUtils;

/**
 * Created by caofuxiang on 2018/08/16.
 */
@Ignore
public class HttpServerPerfTest {

    @Before
    public void setUp() {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(ch.qos.logback.classic.Level.INFO);
    }

    @Test
    @Ignore
    public void testPerf() throws Exception {
        int qps = 20000;
        RateLimiter rateLimiter = RateLimiter.create(qps);
        Meter meter = Metrics.newMeter(HttpServerPerfTest.class, "qps", "requests", TimeUnit.SECONDS);
        Histogram histogram = Metrics.newHistogram(HttpServerPerfTest.class, "latency");

        HttpDispatcher dispatcher = new HttpDispatcher();
        dispatcher.registerController("/hi", (pathParams, context) -> context.send("hello"));
        HttpServer server = new HttpServer(new InetSocketAddress(6080), dispatcher);
        HttpClient client = new HttpClient();
        try {
            server.start();
            ConsoleReporter.enable(2, TimeUnit.SECONDS);

            while (true) {
                rateLimiter.acquire();
                long begin = System.currentTimeMillis();
                HttpRequest request = MessageUtils.newHttpRequest(HttpMethod.GET, "http://localhost:6080/hi");
                client.execute(request).thenApply(r -> {
                    histogram.update(System.currentTimeMillis() - begin);
                    meter.mark();
                    return r;
                }).exceptionally(e -> {
                    e.printStackTrace();
                    System.exit(1);
                    return null;
                });
            }
        } finally {
            client.close(true);
            server.stop(true);
        }
    }
}