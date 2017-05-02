package net.goldolphin.maria.common;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by caofuxiang on 2017/5/2.
 */
public class Futures {
    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor();

    public static CompletableFuture<Void> delayedFuture(long timeout, TimeUnit unit) {
        return delayedFuture(null, timeout, unit);
    }

    public static <T> CompletableFuture<T> delayedFuture(T value, long timeout, TimeUnit unit) {
        CompletableFuture<T> future = new CompletableFuture<>();
        EXECUTOR_SERVICE.schedule(() -> {
            future.complete(value);
        }, timeout, unit);
        return future;
    }

    public static <T> CompletableFuture<T> retry(Supplier<? extends CompletableFuture<T>> supplier,
            int maxRetry, long initialDelay, long maxDelay, TimeUnit unit) {
        return retry(v -> supplier.get(), null, maxRetry, initialDelay, maxDelay, unit, new CompletableFuture<>());
    }

    public static <U, T> CompletableFuture<T> retry(Function<? super U, ? extends CompletableFuture<T>> fn, U u,
            int maxRetry, long initialDelay, long maxDelay, TimeUnit unit) {
        return retry(fn, u, maxRetry, initialDelay, maxDelay, unit, new CompletableFuture<>());
    }

    private static <U, T> CompletableFuture<T> retry(Function<? super U, ? extends CompletableFuture<T>> fn, U u,
            int maxRetry, long initialDelay, long maxDelay, TimeUnit unit, CompletableFuture<T> future) {
        try {
            fn.apply(u).whenComplete((v, e) -> {
                if (e == null) {
                    future.complete(v);
                    return;
                }
                if (maxRetry == 0) {
                    future.completeExceptionally(e);
                    return;
                }
                delayedFuture(u, initialDelay, unit).thenCompose(u1 -> retry(fn, u1,
                        maxRetry - 1, Math.min(2 * initialDelay, maxDelay), maxDelay, unit, future));
            });
        } catch (Throwable e) {
            if (maxRetry == 0) {
                future.completeExceptionally(e);
            } else {
                delayedFuture(u, initialDelay, unit).thenCompose(u1 -> retry(fn, u1,
                        maxRetry - 1, Math.min(2 * initialDelay, maxDelay), maxDelay, unit, future));
            }
        }
        return future;
    }
}
