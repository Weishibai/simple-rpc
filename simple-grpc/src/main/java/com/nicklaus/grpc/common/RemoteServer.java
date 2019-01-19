package com.nicklaus.grpc.common;

import lombok.Data;

/**
 * 远程服务
 */
@Data
public class RemoteServer {

    /**
     * service name
     */
    private String server;

    /**
     * host
     */
    private String host;

    /**
     * port
     */
    private int port;

}
