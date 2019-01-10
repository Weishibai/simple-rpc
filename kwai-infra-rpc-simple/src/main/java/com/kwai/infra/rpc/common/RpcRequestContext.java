package com.kwai.infra.rpc.common;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.kwai.infra.rpc.common.proxy.RpcRequest;
import com.kwai.infra.rpc.common.proxy.RpcResponse;

/**
 * rpc request context
 *
 * @author weishibai
 * @date 2019/01/04 11:41 AM
 */
public class RpcRequestContext implements Closeable {

    private static final ConcurrentMap<String, RpcRequestContext> CONTEXT_POOL = Maps.newConcurrentMap();

    private Future<?> future;

    private RpcRequest request;

    private RpcResponse response;

    private final CountDownLatch latch = new CountDownLatch(1);

    private final Object lock = new Object();

    private volatile boolean done;

    public static RpcRequestContext context(String requestId) {
        RpcRequestContext context = CONTEXT_POOL.get(requestId);
        if (null == context) {
            context = new RpcRequestContext();
            CONTEXT_POOL.putIfAbsent(requestId, context);
            return context;
        }
        return context;
    }

    /* in case of leak */
    public static void removeContext(String requestId) {
        CONTEXT_POOL.remove(requestId);
    }

    /**
     * get future
     * @return future
     */
    @SuppressWarnings("unchecked")
    public <T> Future<T> getFuture() {
        return (Future<T>) future;
    }

    /**
     * set future.
     *
     * @param future
     */
    public void setFuture(Future<?> future) {
        this.future = future;
    }

    public RpcRequest getRequest() {
        return request;
    }

    public void setRequest(RpcRequest request) {
        this.request = request;
    }

    public void setResponse(RpcResponse response) {
        if (done) {
            return;
        }

        // notify future lock
        synchronized (lock) {
            if (done) {
                return;
            }

            done = true;
            this.response = response;
            latch.countDown();
        }
    }

    public RpcResponse get(long millis) {
        try {
            if (latch.await(millis, TimeUnit.MILLISECONDS)) {
                return response;
            }
            throw new UncheckedTimeoutException("wait " + millis + " ms timeout for request " + request);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {

    }
}
