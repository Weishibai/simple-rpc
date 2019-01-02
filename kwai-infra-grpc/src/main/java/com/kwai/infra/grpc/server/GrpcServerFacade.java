package com.kwai.infra.grpc.server;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import com.kwai.infra.grpc.common.GrpcChannelFactory;
import com.kwai.infra.grpc.common.config.GrpcConfigurer;
import com.kwai.infra.grpc.common.internal.InfraService;
import com.kwai.infra.grpc.utils.HostUtils;

import io.grpc.Server;
import io.grpc.ServerInterceptor;

/**
 * grpc provider
 *
 * @author weishibai
 * @date 2018/12/31 1:44 PM
 */
public class GrpcServerFacade implements DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcServerFacade.class);

    /* config */
    private GrpcConfigurer configurer;

    /* real grpc server */
    private Server internalServer;

    /* server interceptors */
    private List<ServerInterceptor> interceptors;

    /* real grpc service */
    private InfraService infraService;

    private AtomicBoolean started = new AtomicBoolean(false);

    private final Object lock = new Object();

    public GrpcServerFacade(InfraService infraService, GrpcConfigurer configurer) {
        this.infraService = infraService;
        this.configurer = configurer;
    }

    public GrpcServerFacade(InfraService infraService, GrpcConfigurer configurer, List<ServerInterceptor> interceptors) {
        this.infraService = infraService;
        this.configurer = configurer;
        this.interceptors = interceptors;
    }

    public void start() {
        if (started.get()) {
            return;
        }

        final int port = configurer.getPort();
        if (port <= 0 || StringUtils.equals(HostUtils.getHostName(), "127.0.0.1")) {
            throw new RuntimeException(String.format("grpc server started failed illegal host %s or port %d"
                    , HostUtils.getHostName(), port));
        }

        synchronized (lock) {
            if (started.get()) {
                return;
            }
            internalServer = GrpcChannelFactory.server(configurer.getPort(), infraService, interceptors);
            try {
                internalServer.start();
                started.compareAndSet(false, true);
                startDaemonAwaitThread();
            } catch (IOException e) {
                throw new RuntimeException("gpr server started failed: ", e);
            }
        }
    }

    @Override
    public void destroy() {
        Optional.ofNullable(internalServer).ifPresent(Server::shutdown);
        LOGGER.info("gRPC server stopped.");
    }

    private void startDaemonAwaitThread() {
        Thread awaitThread = new Thread(() -> {
            try {
                internalServer.awaitTermination();
            } catch (InterruptedException e) {
                LOGGER.warn("gRPC server stopped: ", e);
            }
        });
        awaitThread.setDaemon(false);
        awaitThread.start();
    }
}
