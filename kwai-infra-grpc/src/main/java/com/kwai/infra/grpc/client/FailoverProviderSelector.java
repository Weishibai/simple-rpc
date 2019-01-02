package com.kwai.infra.grpc.client;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

import io.grpc.ManagedChannel;

/**
 * failover provider selector
 *
 * @author weishibai
 * @date 2019/01/01 12:23 PM
 */
public class FailoverProviderSelector implements ProviderSelector {

    private final ConcurrentMap<String, Collection<GrpcServerContext>> serverMap;

    private ScheduledExecutorService scheduledPool;

    public FailoverProviderSelector(ConcurrentMap<String, Collection<GrpcServerContext>> serverMap) {
        this.serverMap = serverMap;
        /* health check */
        this.scheduledPool = Executors.newScheduledThreadPool(1);
        scheduledPool.schedule(() -> {
            /* detect */
            final Multimap<String, GrpcServerContext> removeMap = ArrayListMultimap.create();
            serverMap.forEach((serverKey, list) -> {
                list.forEach(context -> {
                    ManagedChannel channel = context.channel();
                    if (channel.isTerminated()) {
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
            });
        }, 5, TimeUnit.MINUTES);
    }

    @Override
    public GrpcServerContext select(String serverKey) {
        if (!serverMap.containsKey(serverKey)) {
            return null;
        }

        Collection<GrpcServerContext> grpcServerContexts = serverMap.get(serverKey);
        if (CollectionUtils.isEmpty(grpcServerContexts)) {
            return null;
        }
        return Iterables.get(grpcServerContexts, new Random().nextInt(CollectionUtils.size(grpcServerContexts)));
    }

    @Override
    public void close() throws IOException {
        if (!scheduledPool.isShutdown()) {
            scheduledPool.shutdownNow();
        }
        serverMap.values().stream().flatMap(Collection::stream).forEach(GrpcServerContext::closeGracefully);
    }
}
