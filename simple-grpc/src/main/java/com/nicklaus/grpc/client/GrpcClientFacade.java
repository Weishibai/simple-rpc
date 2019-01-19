package com.nicklaus.grpc.client;

import static com.nicklaus.grpc.common.GrpcChannelFactory.client;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.DisposableBean;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.nicklaus.grpc.common.HealthChecker;
import com.nicklaus.grpc.common.RemoteServer;
import com.nicklaus.grpc.common.config.GrpcConfigurer;

import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;

/**
 * grpc client facade
 *
 * @author weishibai
 * @date 2019/01/01 9:49 AM
 */
public class GrpcClientFacade implements DisposableBean {

    private List<ClientInterceptor> clientInterceptors;

    private static ProviderSelector providerSelector;

    private final HealthChecker healthChecker;

    private final ConcurrentMap<String, Collection<GrpcServerContext>> serverMap;

    public GrpcClientFacade(GrpcConfigurer configurer, List<ClientInterceptor> interceptors) {
        Preconditions.checkArgument(null != configurer, "illegal rpc configurer");
        this.clientInterceptors = interceptors;

        this.serverMap = build(configurer, clientInterceptors);
        providerSelector = new RandomProviderSelector(serverMap);

        /* health check */
        healthChecker = new HealthChecker(serverMap);
        healthChecker.check();
    }

    public static GrpcServerContext request(String serverKey) {
        return providerSelector.select(serverKey);
    }

    private ConcurrentMap<String, Collection<GrpcServerContext>> build(GrpcConfigurer configurer, List<ClientInterceptor> clientInterceptors) {
        final ConcurrentMap<String, Collection<GrpcServerContext>> serverMap = Maps.newConcurrentMap();
        for (RemoteServer remoteServer : configurer.getRemoteServers()) {
            ManagedChannel channel = client(remoteServer, clientInterceptors);
            if (serverMap.containsKey(remoteServer.getServer())) {
                serverMap.get(remoteServer.getServer()).add(new GrpcServerContext(channel));
            } else {
                serverMap.put(remoteServer.getServer(), Lists.newArrayList(new GrpcServerContext(channel)));
            }
        }
        return serverMap;
    }

    @Override
    public void destroy() throws Exception {
        try {
            healthChecker.close();
            serverMap.values().stream().flatMap(Collection::stream).forEach(GrpcServerContext::closeGracefully);
        } catch (IOException e) {
            //ig
        }
    }
}
