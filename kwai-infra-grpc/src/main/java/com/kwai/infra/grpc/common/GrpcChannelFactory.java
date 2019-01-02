package com.kwai.infra.grpc.common;

import static io.grpc.netty.shaded.io.grpc.netty.NegotiationType.PLAINTEXT;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MINUTES;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import io.grpc.BindableService;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.channel.ChannelOption;

/**
 * grpc channel builder
 *
 * @author weishibai
 * @date 2018/12/29 6:56 PM
 */
public class GrpcChannelFactory {

    public static Server server(int port, BindableService service, List<ServerInterceptor> interceptors) {
        if (null == interceptors || interceptors.isEmpty()) {
            return server(port, service);
        }
        return ServerBuilder.forPort(port).addService(ServerInterceptors.intercept(service, interceptors))
                .directExecutor().build();
    }

    public static Server server(int port, BindableService service) {
        return ServerBuilder.forPort(port).addService(service).build();
    }

    public static ManagedChannel client(RemoteServer server) {
        return NettyChannelBuilder.forAddress(server.getHost(), server.getPort())
                .usePlaintext().idleTimeout(1, DAYS).build();
    }

    public static ManagedChannel client(GrpcChannelBuilder channelBuilder) {
        NettyChannelBuilder builder = NettyChannelBuilder.forAddress(channelBuilder.getHost(), channelBuilder.getPort())
                .usePlaintext();

        if (channelBuilder.getConnectTimeOut() > 0) {
            builder.withOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, channelBuilder.getConnectTimeOut());
        }

        /* days */
        if (channelBuilder.getIdleTimeOut() > 0) {
            builder.idleTimeout(channelBuilder.getIdleTimeOut(), MINUTES); // 默认是30分钟，改成1天
        } else {
            builder.idleTimeout(1, DAYS); // 默认是30分钟，改成1天
        }

        if (channelBuilder.getMaxMessageSize() > 0) {
            builder.maxInboundMessageSize(channelBuilder.getMaxMessageSize());
        }

        if (CollectionUtils.isNotEmpty(channelBuilder.getInterceptors())) {
            for (ClientInterceptor clientInterceptor : channelBuilder.getInterceptors()) {
                builder.intercept(clientInterceptor);
            }
        }
        return builder.build();
    }

    public static ManagedChannel client(RemoteServer server, List<ClientInterceptor> clientInterceptors) {
        NettyChannelBuilder builder = NettyChannelBuilder.forAddress(server.getHost(), server.getPort());

        /* current no tls */
        builder.negotiationType(PLAINTEXT);

        if (CollectionUtils.isNotEmpty(clientInterceptors)) {
            for (ClientInterceptor clientInterceptor : clientInterceptors) {
                builder.intercept(clientInterceptor);
            }
        }
        builder.idleTimeout(1, DAYS); // 默认是30分钟，改成1天
        return builder.build();
    }

}
