package com.nicklaus.grpc.common;

import java.util.List;

import com.google.common.collect.Lists;

import io.grpc.ClientInterceptor;

/**
 * grpc channel builder
 *
 * @author weishibai
 * @date 2019/01/01 4:59 PM
 */
public class GrpcChannelBuilder {

    private List<ClientInterceptor> interceptors;

    private int port;

    private String host;

    private int maxMessageSize;

    private int idleTimeOut = 1; //minutes

    private int connectTimeOut;

    public static GrpcChannelBuilder newBuilder() {
        return new GrpcChannelBuilder();
    }

    public GrpcChannelBuilder addClientInterceptor(ClientInterceptor interceptor) {
        if (null == interceptors) {
            interceptors = Lists.newArrayList();
        }
        interceptors.add(interceptor);
        return this;
    }

    public GrpcChannelBuilder forAddress(String host, int port) {
        this.host = host;
        this.port = port;
        return this;
    }

    public GrpcChannelBuilder idleTimeOut(int idleTimeOut) {
        this.idleTimeOut = idleTimeOut;
        return this;
    }

    public GrpcChannelBuilder maxMessageSize(int size) {
        this.maxMessageSize = size;
        return this;
    }

    public GrpcChannelBuilder connectTimeOut(int connectTimeOut) {
        this.connectTimeOut = connectTimeOut;
        return this;
    }

    public List<ClientInterceptor> getInterceptors() {
        return interceptors;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public int getMaxMessageSize() {
        return maxMessageSize;
    }

    public int getIdleTimeOut() {
        return idleTimeOut;
    }

    public int getConnectTimeOut() {
        return connectTimeOut;
    }
}
