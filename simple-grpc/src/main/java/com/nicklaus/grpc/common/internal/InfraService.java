package com.nicklaus.grpc.common.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.cglib.reflect.FastClass;
import org.springframework.cglib.reflect.FastMethod;
import org.springframework.context.support.AbstractApplicationContext;

import com.google.protobuf.ByteString;
import com.nicklaus.grpc.InfraServiceGrpc;
import com.nicklaus.grpc.utils.ProtobufUtils;
import com.nicklaus.grpc.GrpcInfraService;
import com.nicklaus.grpc.utils.BeanUtils;

import io.grpc.stub.StreamObserver;

/**
 * infra service
 *
 * @author weishibai
 * @date 2019/01/01 9:33 AM
 */
public class InfraService extends InfraServiceGrpc.InfraServiceImplBase {

    private final AbstractApplicationContext applicationContext;

    public InfraService(AbstractApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void handle(GrpcInfraService.InfraRequest request, StreamObserver<GrpcInfraService.InfraResponse> responseObserver) {
        GrpcRequest localRequest = deserialize(request);
        GrpcResponse response = new GrpcResponse();
        try {
            String className = localRequest.getClazz();
            Object bean = BeanUtils.getBean(Class.forName(className), applicationContext);
            Object[] args = localRequest.getArgs();
            Class[] argsTypes = BeanUtils.getParameterTypes(args);
            Method matchingMethod = MethodUtils.getMatchingMethod(Class.forName(className), localRequest.getMethod(), argsTypes);
            FastClass serviceFastClass = FastClass.create(bean.getClass());
            FastMethod serviceFastMethod = serviceFastClass.getMethod(matchingMethod);
            Object result = serviceFastMethod.invoke(bean, args);
            response.success(result);
        } catch (NoSuchBeanDefinitionException | ClassNotFoundException | InvocationTargetException exception) {
            String message = exception.getClass().getName() + ": " + exception.getMessage();
            response.error(message, exception, exception.getStackTrace());
        }

        ByteString bytes = serialize(response);
        responseObserver.onNext(GrpcInfraService.InfraResponse.newBuilder().setResponse(bytes).build());
        responseObserver.onCompleted();

    }

    private GrpcRequest deserialize(GrpcInfraService.InfraRequest request) {
        return ProtobufUtils.deserialize(request.getRequest().toByteArray(), GrpcRequest.class);
    }

    private ByteString serialize(GrpcResponse response) {
        return ByteString.copyFrom(ProtobufUtils.serialize(response));
    }

}
