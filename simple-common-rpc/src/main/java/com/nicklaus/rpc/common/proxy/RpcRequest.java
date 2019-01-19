package com.nicklaus.rpc.common.proxy;

import java.io.Serializable;

import lombok.Data;

@Data
public class RpcRequest implements Serializable {

    private static final long serialVersionUID = 4729940126314117605L;

    /**
     * 接口
     */
    private String clazz;

    /**
     * 方法
     */
    private String method;

    /**
     * service 方法参数
     */
    private Object[] args;

    /**
     * use async way or not
     */
    private boolean async;

    /**
     * unique request id
     */
    private String requestId;

}
