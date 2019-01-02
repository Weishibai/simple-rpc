package com.kwai.infra.grpc.client;

/**
 * provider selector interface
 *
 * @author weishibai
 * @date 2018/12/30 10:18 AM
 */
public interface ProviderSelector {

    GrpcServerContext select(String serverKey);

}
