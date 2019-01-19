package com.nicklaus.rpc.client;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.CollectionUtils;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.nicklaus.rpc.common.RemoteServer;
import com.nicklaus.rpc.common.RpcRequestContext;
import com.nicklaus.rpc.common.RpcServerContext;
import com.nicklaus.rpc.common.exception.RpcException;
import com.nicklaus.rpc.common.proxy.RpcRequest;
import com.nicklaus.rpc.common.proxy.RpcResponse;
import com.nicklaus.rpc.common.utils.ObjectMapperUtils;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPromise;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.concurrent.DefaultThreadFactory;

/**
 * http rpc client
 *
 * @author weishibai
 * @date 2019/01/04 11:39 AM
 */
public class HttpRpcClient implements RpcClient {

    private static final int DEFAULT_IO_THREADS = Math.min(Runtime.getRuntime().availableProcessors() + 1, 32);

    private final ConcurrentMap<String, Collection<RpcServerContext>> serverMap;

    private Bootstrap bootstrap;

    private static final NioEventLoopGroup nioEventLoopGroup = new NioEventLoopGroup(DEFAULT_IO_THREADS
            , new DefaultThreadFactory("httpClientWorker", true));

    public HttpRpcClient(Collection<RemoteServer> servers) {
        bootstrap = new Bootstrap();
        bootstrap.group(nioEventLoopGroup)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .channel(NioSocketChannel.class);

        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new HttpResponseDecoder());
                ch.pipeline().addLast(new HttpRequestEncoder());
                ch.pipeline().addLast(new ChannelDuplexHandler() {
                    @Override
                    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

                    }

                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                        if (msg instanceof HttpContent) {
                            HttpContent content = (HttpContent) msg;
                            RpcResponse response = ObjectMapperUtils.fromJSON(content.content().array(), RpcResponse.class);
                            String requestId = response.getRequestId();
                            RpcRequestContext context = RpcRequestContext.context(requestId);
                            context.setResponse(response);
                        }
                    }
                });
            }
        });

        serverMap = Maps.newConcurrentMap();
        for (RemoteServer server : servers) {
            RpcServerContext context = new RpcServerContext() {

                private Channel channel;

                @Override
                public void start() {
                    channel = bootstrap.connect(server.getHost(), server.getPort()).channel();
                }

                @Override
                public void request(RpcRequest request) throws RpcException {
                    try {
                        URI uri = new URI("http://" + server.getHost() + ":" + server.getPort());
                        FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri.toASCIIString()
                                , Unpooled.wrappedBuffer(ObjectMapperUtils.toJSON(request).getBytes("UTF-8")));
                        httpRequest.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);

                        ChannelFuture channelFuture = channel.writeAndFlush(httpRequest);
                        boolean success = channelFuture.await(3, TimeUnit.SECONDS);

                        Throwable cause = channelFuture.cause();
                        if (cause != null) {
                            throw new RpcException(cause);
                        }

                        if (!success) {
                            throw new RpcException("failed to send request " + request + " to " + channel.remoteAddress());
                        }
                    } catch (URISyntaxException | UnsupportedEncodingException | InterruptedException e) {
                        throw new RpcException(e);
                    }
                }

                @Override
                public void closeGracefully() {
                    try {
                        channel.close().sync().get();
                    } catch (Throwable e) {
                        //ig
                    }
                }
            };
            /* start channel */
            context.start();
            if (serverMap.containsKey(server.getServer())) {
                serverMap.get(server.getServer()).add(context);
            } else {
                serverMap.put(server.getServer(), Collections.singleton(context));
            }
        }
    }

    @Override
    public RpcRequestContext request(RpcRequest request, String key) {
        Collection<RpcServerContext> servers = serverMap.get(key);
        if (CollectionUtils.isEmpty(servers)) {
            throw new RuntimeException("no avaliable server of " + key);
        }

        RpcServerContext remoteServer = Iterables.get(servers, new Random().nextInt(CollectionUtils.size(servers)));
        remoteServer.request(request);
        RpcRequestContext context = RpcRequestContext.context(request.getRequestId());
        context.setRequest(request);
        return context;
    }

    @Override
    public void destroy() throws Exception {

    }
}
