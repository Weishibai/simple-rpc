package com.kwai.infra.rpc.common;

import com.kwai.infra.rpc.common.exception.RpcException;
import com.kwai.infra.rpc.common.proxy.RpcRequest;

/**
 * rpc server context
 *
 * @author weishibai
 * @date 2019/01/04 1:02 PM
 */
public interface RpcServerContext {

    void start();

    void request(RpcRequest request) throws RpcException;

    void closeGracefully();

}
