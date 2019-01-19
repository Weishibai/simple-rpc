package com.kwai.infra.rpc.common.utils;

/**
 * rpc utils
 *
 * @author weishibai
 * @date 2019/01/04 4:11 PM
 */
public class RpcUtils {

    public static String buildRequestId(String className, String method) {
        return className + "." + method + "/" + System.currentTimeMillis() + "/" + HostUtils.getHostName();
    }
}
