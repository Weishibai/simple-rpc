package com.kwai.infra.grpc.common;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.kwai.infra.grpc.client.GrpcServerContext;

import io.grpc.ManagedChannel;

/**
 * health checker to providers
 *
 * @author weishibai
 * @date 2018/12/30 10:30 AM
 */
public class HealthChecker implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(HealthChecker.class);

    private ScheduledExecutorService scheduledPool;

    private final ConcurrentMap<String, Collection<GrpcServerContext>> serverMap;

    public HealthChecker(ConcurrentMap<String, Collection<GrpcServerContext>> serverMap) {
        this.serverMap = serverMap;
        this.scheduledPool = Executors.newScheduledThreadPool(1);
    }

    public void check() {
        scheduledPool.schedule(() -> {
            /* detect */
            final Multimap<String, GrpcServerContext> removeMap = ArrayListMultimap.create();
            serverMap.forEach((serverKey, list) -> {
                list.forEach(context -> {
                    ManagedChannel channel = context.channel();
                    if (channel.isTerminated() || channel.isShutdown()) {
                        removeMap.put(serverKey, context);
                    }
                });
            });

            /* failover */
            removeMap.entries().forEach(entry -> {
                Collection<GrpcServerContext> contexts = serverMap.get(entry.getKey());
                Iterables.removeAll(contexts, Collections.singletonList(entry.getValue()));
                serverMap.put(entry.getKey(), contexts);
                /* clear resource */
                entry.getValue().closeGracefully();
                LOGGER.info("server key {} -- remove failed instance: {}", entry.getValue());
            });
        }, 10, TimeUnit.SECONDS);
    }

    @Override
    public void close() throws IOException {
        if (!scheduledPool.isShutdown()) {
            scheduledPool.shutdownNow();
        }
    }
}
