package com.nicklaus.rpc.client;

import org.springframework.beans.factory.DisposableBean;

import com.nicklaus.rpc.common.RpcRequestContext;
import com.nicklaus.rpc.common.proxy.RpcRequest;

/**
 * client interface
 *
 * @author weishibai
 * @date 2019/01/04 11:35 AM
 */
public interface RpcClient extends DisposableBean {

    RpcRequestContext request(RpcRequest request, String key);
}
