package com.kwai.infra.rpc.server;

import org.springframework.beans.factory.DisposableBean;

import com.kwai.infra.rpc.common.proxy.RpcRequest;
import com.kwai.infra.rpc.common.proxy.RpcResponse;

/**
 * rpc server exporter
 *
 * @author weishibai
 * @date 2019/01/04 12:55 PM
 */
public interface RpcServer extends DisposableBean {

    RpcResponse invoke(RpcRequest request);

}
