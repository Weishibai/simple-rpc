package com.kwai.infra.rpc.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

import com.kwai.infra.rpc.common.config.RpcServiceScannerRegistry;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({RpcServiceScannerRegistry.class})
public @interface RpcServiceScanner {

    /**
     * `@RpcService` 所注解的包扫描路径
     */
    String[] packages() default {};

}