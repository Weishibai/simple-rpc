package com.nicklaus.grpc.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;

import com.nicklaus.grpc.common.annotation.GrpcService;
import com.nicklaus.grpc.common.internal.GrpcRequest;
import com.nicklaus.grpc.common.internal.GrpcResponse;

/**
 * grpc service proxy
 *
 * @author weishibai
 * @date 2018/12/31 11:32 AM
 */
public class GrpcServiceProxy<T> implements InvocationHandler {

    private static final int DEFAULT_REQUEST_TIME_OUT = 5;

    private Class<T> grpcService;

    private Object invoker;

    public GrpcServiceProxy(Class<T> grpcService, Object invoker) {
        this.grpcService = grpcService;
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

        GrpcService annotation = grpcService.getAnnotation(GrpcService.class);
        String server = annotation.serverName();
        GrpcRequest request = new GrpcRequest();
        request.setClazz(className);
        request.setMethod(methodName);
        request.setArgs(args);

        GrpcResponse response = GrpcClientFacade.request(server).sync(request, DEFAULT_REQUEST_TIME_OUT);
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
    }

}
