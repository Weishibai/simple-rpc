package com.nicklaus.rpc.common.proxy;

import java.io.Serializable;

import lombok.Data;

@Data
public class RpcResponse implements Serializable {

    private static final long serialVersionUID = -7161518426386434816L;

    /**
     * 信息提示
     */
    private String message;

    /**
     * 返回结果
     */
    private Object result;

    /**
     * 服务端异常
     */
    private Throwable exception;

    /**
     * 异常堆栈信息
     */
    private StackTraceElement[] stackTrace;

    /* from which request */
    private String requestId;

    public void error(String message, Throwable exception, StackTraceElement[] stackTrace) {
        this.message = message;
        this.exception = exception;
        this.stackTrace = stackTrace;
    }

    public void success(Object result) {
        this.result = result;
    }
}
