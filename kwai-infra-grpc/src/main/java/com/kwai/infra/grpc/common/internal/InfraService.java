package com.kwai.infra.grpc.common.internal;

import static com.kwai.infra.grpc.GrpcInfraService.InfraRequest;
import static com.kwai.infra.grpc.GrpcInfraService.InfraResponse;
import static com.kwai.infra.grpc.utils.BeanUtils.getBean;
import static com.kwai.infra.grpc.utils.BeanUtils.getParameterTypes;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.cglib.reflect.FastClass;
import org.springframework.cglib.reflect.FastMethod;
import org.springframework.context.support.AbstractApplicationContext;

import com.google.protobuf.ByteString;
import com.kwai.infra.grpc.InfraServiceGrpc;
import com.kwai.infra.grpc.utils.ProtobufUtils;

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
    public void handle(InfraRequest request, StreamObserver<InfraResponse> responseObserver) {
        GrpcRequest localRequest = deserialize(request);
        GrpcResponse response = new GrpcResponse();
        try {
            String className = localRequest.getClazz();
            Object bean = getBean(Class.forName(className), applicationContext);
            Object[] args = localRequest.getArgs();
            Class[] argsTypes = getParameterTypes(args);
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
        responseObserver.onNext(InfraResponse.newBuilder().setResponse(bytes).build());
        responseObserver.onCompleted();

    }

    private GrpcRequest deserialize(InfraRequest request) {
        return ProtobufUtils.deserialize(request.getRequest().toByteArray(), GrpcRequest.class);
    }

    private ByteString serialize(GrpcResponse response) {
        return ByteString.copyFrom(ProtobufUtils.serialize(response));
    }

}
