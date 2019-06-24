package net.goldolphin.maria;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import io.netty.handler.codec.http.HttpMethod;
import net.goldolphin.maria.common.MessageUtils;

/**
 * Created by caofuxiang on 2019/06/24.
 */
public class HttpClientTest {
    @Test
    public void testTimeout() throws ExecutionException, InterruptedException {
        long begin = 0;
        Duration timeout = Duration.ofMillis(500);
        try (HttpClient client = new HttpClient()) {
            begin = System.currentTimeMillis();
            client.execute(MessageUtils.newHttpRequest(HttpMethod.GET, "http://127.0.0.2:12345"), timeout).get();
        } catch (Exception ignored){
            long elapsed = System.currentTimeMillis() - begin;
            System.out.println("Time elapsed: " + elapsed + "ms");
            Assert.assertTrue(elapsed <= timeout.toMillis() + 1000);
            return;
        }
        Assert.fail();
    }
}