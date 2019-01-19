package com.nicklaus.grpc.common.annotation;

import static com.nicklaus.grpc.common.config.GrpcAutoConfiguration.ExternalGrpcServiceScannerRegister;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({ExternalGrpcServiceScannerRegister.class})
public @interface GrpcServiceScanner {

    /**
     * `@GrpcService` 所注解的包扫描路径
     */
    String[] packages() default {};

}