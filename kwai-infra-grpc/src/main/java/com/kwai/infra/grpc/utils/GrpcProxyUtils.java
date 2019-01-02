package com.kwai.infra.grpc.utils;

import java.lang.reflect.InvocationHandler;
import java.util.Collection;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import com.google.common.reflect.Reflection;
import com.kwai.infra.grpc.client.GrpcServiceProxy;

import lombok.extern.slf4j.Slf4j;

/**
 * grpc proxy utils
 *
 * @author weishibai
 * @date 2018/12/31 1:04 PM
 */
@Slf4j
public class GrpcProxyUtils {

    public static void registerBeans(BeanFactory beanFactory, Collection<BeanDefinition> beanDefinitions) {
        for (BeanDefinition beanDefinition : beanDefinitions) {
            String className = beanDefinition.getBeanClassName();
            if (StringUtils.isBlank(className)) {
                continue;
            }
            try {
                // 创建代理类
                Class<?> target = Class.forName(className);
                InvocationHandler invocationHandler = new GrpcServiceProxy<>(target, new Object());
                Object proxy = Reflection.newProxy(target, invocationHandler);

                // 注册到 Spring 容器
                ((DefaultListableBeanFactory) beanFactory).registerSingleton(toCamelCase(className), proxy);
            } catch (ClassNotFoundException e) {
                log.warn("class not found : {}", className);
            }
        }
    }

    private static String toCamelCase(String str) {
        String name = ClassUtils.getShortCanonicalName(str);
        return StringUtils.lowerCase(String.valueOf(name.charAt(0))) + name.substring(1);
    }
}
