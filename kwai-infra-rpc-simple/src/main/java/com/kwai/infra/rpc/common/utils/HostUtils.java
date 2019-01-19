package com.kwai.infra.rpc.common.utils;

import static java.net.InetAddress.getLocalHost;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * host utils
 *
 * @author weishibai
 * @date 2019/01/01 10:13 AM
 */
public class HostUtils {

    private static final String HOST_NAME;

    static {
        try {
            InetAddress host = getLocalHost();
            HOST_NAME = host != null ? host.getHostName() : null;
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getHostName() {
        return HOST_NAME;
    }

}
