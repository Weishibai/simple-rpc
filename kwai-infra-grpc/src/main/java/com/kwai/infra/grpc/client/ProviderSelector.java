package com.kwai.infra.grpc.client;

import java.io.Closeable;

/**
 * provider selector interface
 *
 * @author weishibai
 * @date 2018/12/30 10:18 AM
 */
public interface ProviderSelector extends Closeable {

    GrpcServerContext select(String serverKey);

}
