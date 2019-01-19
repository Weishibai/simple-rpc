package com.nicklaus.rpc.server;

import org.springframework.beans.factory.DisposableBean;

import com.nicklaus.rpc.common.proxy.RpcRequest;
import com.nicklaus.rpc.common.proxy.RpcResponse;

/**
 * rpc server exporter
 *
 * @author weishibai
 * @date 2019/01/04 12:55 PM
 */
public interface RpcServer extends DisposableBean {

    RpcResponse invoke(RpcRequest request);

}
