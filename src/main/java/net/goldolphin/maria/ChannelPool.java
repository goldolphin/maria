package net.goldolphin.maria;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

/**
 * Created by caofuxiang on 2018/08/16.
 */
public class ChannelPool implements AutoCloseable {
    private final int capacity;
    private final ConcurrentHashMap<InetSocketAddress, BlockingQueue<Channel>> map = new ConcurrentHashMap<>();

    public ChannelPool(int capacity) {
        this.capacity = capacity;
    }

    private BlockingQueue<Channel> getBucket(InetSocketAddress remoteAddress) {
        return map.computeIfAbsent(remoteAddress, a -> new ArrayBlockingQueue<>(capacity));
    }

    public Channel acquire(InetSocketAddress remoteAddress) {
        if (capacity == 0) {
            return null;
        }
        return getBucket(remoteAddress).poll();
    }

    public void release(InetSocketAddress remoteAddress, Channel channel) {
        if (capacity == 0) {
            channel.close();
            return;
        }
        BlockingQueue<Channel> bucket = getBucket(remoteAddress);
        if (!bucket.offer(channel)) {
            channel.close();
        }
    }

    @Override
    public void close() {
        List<ChannelFuture> futures = new ArrayList<>();
        for (BlockingQueue<Channel> bucket : map.values()) {
            for (Channel channel : bucket) {
                futures.add(channel.close());
            }
        }
        for (ChannelFuture f : futures) {
            f.syncUninterruptibly();
        }
    }
}
