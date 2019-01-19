package com.nicklaus.grpc.common.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.nicklaus.grpc.common.RemoteServer;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "spring.grpc")
public class GrpcConfigurer {

    private boolean enable;

    private int port;

    private List<RemoteServer> remoteServers;

}