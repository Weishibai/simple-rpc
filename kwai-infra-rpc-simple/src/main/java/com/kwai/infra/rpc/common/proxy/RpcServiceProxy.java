package com.kwai.infra.rpc.common.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;

import com.kwai.infra.rpc.client.RpcClient;
import com.kwai.infra.rpc.common.RpcRequestContext;
import com.kwai.infra.rpc.common.annotation.RpcService;
import com.kwai.infra.rpc.common.utils.RpcUtils;

/**
 * grpc service proxy
 *
 * @author weishibai
 * @date 2018/12/31 11:32 AM
 */
public class RpcServiceProxy<T> implements InvocationHandler {

    private static final int DEFAULT_REQUEST_TIME_OUT = 5000;

    private RpcClient rpcClient;

    private Class<T> grpcService;

    private Object invoker;

    public RpcServiceProxy(Class<T> grpcService, RpcClient rpcClient, Object invoker) {
        this.grpcService = grpcService;
        this.rpcClient = rpcClient;
        this.invoker = invoker;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        String className = grpcService.getName();
        if ("toString".equals(methodName) && args.length == 0) {
            return className + "@" + invoker.hashCode();
        } else if ("hashCode".equals(methodName) && args.length == 0) {
            return invoker.hashCode();
        } else if ("equals".equals(methodName) && args.length == 1) {
            Object another = args[0];
            return proxy == another;
        }

        RpcService annotation = grpcService.getAnnotation(RpcService.class);
        RpcRequest request = new RpcRequest();
        request.setClazz(className);
        request.setMethod(methodName);
        request.setArgs(args);
        request.setAsync(false);
        request.setRequestId(RpcUtils.buildRequestId(className, methodName));


        try {
        /* default use http protocol client */
            RpcRequestContext context = rpcClient.request(request, annotation.serverName());
            RpcResponse response = context.get(DEFAULT_REQUEST_TIME_OUT);
            if (response.getException() != null) {
                Throwable throwable = response.getException();
                RuntimeException exception = new RuntimeException(throwable.getClass().getName() + ": " + throwable.getMessage());
                StackTraceElement[] exceptionStackTrace = exception.getStackTrace();
                StackTraceElement[] responseStackTrace = response.getStackTrace();
                StackTraceElement[] allStackTrace = Arrays.copyOf(exceptionStackTrace
                        , exceptionStackTrace.length + responseStackTrace.length);
                System.arraycopy(responseStackTrace, 0, allStackTrace, exceptionStackTrace.length, responseStackTrace.length);
                exception.setStackTrace(allStackTrace);
                throw exception;
            }
            return response.getResult();
        } finally {
            RpcRequestContext.removeContext(request.getRequestId());
        }
    }

}
