package com.nicklaus.grpc.client;

import java.util.Collection;
import java.util.Random;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.collections4.CollectionUtils;

import com.google.common.collect.Iterables;

/**
 * failover provider selector
 *
 * @author weishibai
 * @date 2019/01/01 12:23 PM
 */
public class RandomProviderSelector implements ProviderSelector {

    private final ConcurrentMap<String, Collection<GrpcServerContext>> serverMap;

    public RandomProviderSelector(ConcurrentMap<String, Collection<GrpcServerContext>> serverMap) {
        this.serverMap = serverMap;
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

}
