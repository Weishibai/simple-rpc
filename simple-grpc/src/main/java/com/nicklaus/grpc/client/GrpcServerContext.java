package com.nicklaus.grpc.client;

import static com.nicklaus.grpc.InfraServiceGrpc.InfraServiceFutureStub;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.protobuf.ByteString;
import com.nicklaus.grpc.GrpcInfraService;
import com.nicklaus.grpc.InfraServiceGrpc;
import com.nicklaus.grpc.common.internal.GrpcRequest;
import com.nicklaus.grpc.common.internal.GrpcResponse;
import com.nicklaus.grpc.utils.ProtobufUtils;

import io.grpc.ManagedChannel;

/**
 * grpc server context
 *
 * @author weishibai
 * @date 2019/01/01 10:53 AM
 */
public class GrpcServerContext implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcServerContext.class);

    private final ManagedChannel channel;

    private final InfraServiceFutureStub futureStub;

    private final AtomicBoolean closed = new AtomicBoolean(false);

    GrpcServerContext(ManagedChannel channel) {
        this.channel = channel;
        this.futureStub = InfraServiceGrpc.newFutureStub(channel);
    }

    public ManagedChannel channel() {
        return channel;
    }

    public ListenableFuture<GrpcInfraService.InfraResponse> async(GrpcRequest localRequest) {
        ByteString bytes = ByteString.copyFrom(ProtobufUtils.serialize(localRequest));
        final long st = System.currentTimeMillis();
        ListenableFuture<GrpcInfraService.InfraResponse> future = futureStub.handle(GrpcInfraService.InfraRequest.newBuilder()
                .setRequest(bytes).build());
        future.addListener(() -> LOGGER.info("service {} costs {} ms", localRequest, System.currentTimeMillis() - st)
                , MoreExecutors.directExecutor());
        return future;
    }

    public GrpcResponse sync(GrpcRequest localRequest, int timeOutSecs) {
        ListenableFuture<GrpcInfraService.InfraResponse> future = async(localRequest);
        try {
            GrpcInfraService.InfraResponse infraResponse = future.get(timeOutSecs, TimeUnit.SECONDS);
            return ProtobufUtils.deserialize(infraResponse.getResponse().toByteArray(), GrpcResponse.class);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new UncheckedTimeoutException(e);
        }
    }

    /* ig concurrent problem */
    public void closeGracefully() {
        if (closed.get()) {
            return;
        }

        try {
            close();
        } catch (IOException e) {
            //ig
        }
    }

    @Override
    public void close() throws IOException {
        if (!channel.isTerminated()) {
            channel.shutdownNow();
            closed.compareAndSet(false, true);
        }
    }

    @Override
    public String toString() {
        return "channel state " + closed.get()
                + " , remote server " + channel.toString();
    }
}
