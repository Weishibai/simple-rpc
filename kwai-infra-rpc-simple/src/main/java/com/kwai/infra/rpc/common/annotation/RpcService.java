package com.kwai.infra.rpc.common.annotation;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * grpc service annotation
 *
 * @author weishibai
 * @date 2018/12/31 12:08 PM
 */
@Documented
@Inherited
@Retention(RUNTIME)
@Target(ElementType.TYPE)
public @interface RpcService {

    /* uniq grpc service name */
    String serverName();

}
