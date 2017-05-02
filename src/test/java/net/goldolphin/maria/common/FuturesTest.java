package net.goldolphin.maria.common;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by caofuxiang on 2017/5/2.
 */
public class FuturesTest {

    @Test
    public void testRetrySync() throws Exception {
        int maxRetry = 3;
        AtomicInteger count = new AtomicInteger(0);
        Futures.retry(() -> {
            count.incrementAndGet();
            System.out.println("fail sync");
            throw new RuntimeException("fail sync");
        }, maxRetry, 200, 2000, TimeUnit.MILLISECONDS).exceptionally(e -> {
            System.out.println(e);
            return null;
        }).get();
        Assert.assertEquals(count.get(), maxRetry + 1);
    }

    @Test
    public void testRetryAsync() throws Exception {
        int maxRetry = 3;
        AtomicInteger count = new AtomicInteger(0);
        Futures.retry(() -> CompletableFuture.runAsync(() -> {
            count.incrementAndGet();
            System.out.println("fail async");
            throw new RuntimeException("fail async");
        }), maxRetry, 200, 2000, TimeUnit.MILLISECONDS).exceptionally(e -> {
            System.out.println(e);
            return null;
        }).get();
        Assert.assertEquals(count.get(), maxRetry + 1);
    }

    @Test
    public void testSucceed() throws Exception {
        int maxRetry = 3;
        AtomicInteger count = new AtomicInteger(0);
        Futures.retry(() -> CompletableFuture.runAsync(() -> {
            count.incrementAndGet();
            System.out.println("succeed");
        }), maxRetry, 200, 2000, TimeUnit.MILLISECONDS).exceptionally(e -> null).get();
        Assert.assertEquals(count.get(), 1);
    }
}
