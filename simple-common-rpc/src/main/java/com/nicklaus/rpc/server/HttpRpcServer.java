package com.nicklaus.rpc.server;

import static com.nicklaus.rpc.common.utils.BeanUtils.getBean;
import static com.nicklaus.rpc.common.utils.BeanUtils.getParameterTypes;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.cglib.reflect.FastClass;
import org.springframework.cglib.reflect.FastMethod;
import org.springframework.context.support.AbstractApplicationContext;

import com.nicklaus.rpc.common.proxy.RpcRequest;
import com.nicklaus.rpc.common.proxy.RpcResponse;
import com.nicklaus.rpc.common.utils.ObjectMapperUtils;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

/**
 * http rpc server
 *
 * @author weishibai
 * @date 2019/01/04 2:54 PM
 */
public class HttpRpcServer implements RpcServer {

    private final ServerBootstrap bootstrap;

    private final AbstractApplicationContext applicationContext;

    public HttpRpcServer(AbstractApplicationContext applicationContext, int port) {
        this.applicationContext = applicationContext;
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        this.bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new HttpResponseEncoder());
                        ch.pipeline().addLast(new HttpRequestDecoder());
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            private volatile HttpRequest httpRequest;

                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                if (msg instanceof HttpRequest) {
                                    httpRequest = (HttpRequest) msg;
                                }

                                if (msg instanceof HttpContent) {
                                    HttpContent content = (HttpContent) msg;
                                    RpcRequest request = ObjectMapperUtils.fromJSON(content.content().array(), RpcRequest.class);
                                    RpcResponse response = invoke(request);
                                    FullHttpResponse httpResponse = new DefaultFullHttpResponse(HTTP_1_1, OK
                                            , Unpooled.wrappedBuffer(ObjectMapperUtils.toJSON(response).getBytes("UTF-8")));
                                    httpResponse.headers().set(CONTENT_TYPE, "text/plain");
                                    httpResponse.headers().set(CONTENT_LENGTH, httpResponse.content().readableBytes());
                                    if (HttpHeaders.isKeepAlive(httpRequest)) {
                                        httpResponse.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
                                    }
                                    ctx.writeAndFlush(httpResponse);
                                }
                            }
                        });
                    }
                }).option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
        /* start backend */
        startDaemonAwaitThread(bootstrap, port);
    }

    @Override
    public RpcResponse invoke(RpcRequest request) {
        RpcResponse response = new RpcResponse();
        try {
            String className = request.getClazz();
            Object[] args = request.getArgs();
            Class[] argsTypes = getParameterTypes(args);
            Method matchingMethod = MethodUtils.getMatchingMethod(Class.forName(className), request.getMethod(), argsTypes);

            Object bean = getBean(Class.forName(className), applicationContext);
            FastClass serviceFastClass = FastClass.create(bean.getClass());
            FastMethod serviceFastMethod = serviceFastClass.getMethod(matchingMethod);
            Object result = serviceFastMethod.invoke(bean, args);
            response.success(result);
            response.setRequestId(request.getRequestId());
        } catch (NoSuchBeanDefinitionException | ClassNotFoundException | InvocationTargetException exception) {
            String message = exception.getClass().getName() + ": " + exception.getMessage();
            response.error(message, exception, exception.getStackTrace());
        }
        return response;
    }

    private void startDaemonAwaitThread(ServerBootstrap b, int port) {
        Thread awaitThread = new Thread(() -> {
            try {
                ChannelFuture future = b.bind(port).sync();
                future.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                //ig
            }
        });
        awaitThread.setDaemon(false);
        awaitThread.start();
    }


    @Override
    public void destroy() throws Exception {
        bootstrap.group().shutdownGracefully();
        bootstrap.childGroup().shutdownGracefully();
    }
}
