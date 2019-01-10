package com.kwai.infra.rpc.common.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.support.AbstractApplicationContext;

/**
 * bean utils
 *
 * @author weishibai
 * @date 2019/01/01 9:36 AM
 */
public class BeanUtils {

    private static Map<Class, Object> serviceBeanMap = new ConcurrentHashMap<>();

    /**
     * 获取 Service Bean
     */
    public static Object getBean(Class<?> clazz, AbstractApplicationContext applicationContext) throws NoSuchBeanDefinitionException {
        if (serviceBeanMap.containsKey(clazz)) {
            return serviceBeanMap.get(clazz);
        }
        try {
            Object bean = applicationContext.getBean(clazz);
            serviceBeanMap.put(clazz, bean);
            return bean;
        } catch (BeansException e) {
            throw new NoSuchBeanDefinitionException(clazz);
        }
    }

    /**
     * 获取参数类型
     */
    public static Class[] getParameterTypes(Object[] parameters) {
        if (parameters == null) {
            return null;
        }
        Class[] clazzArray = new Class[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            clazzArray[i] = parameters[i].getClass();

        }
        return clazzArray;
    }

}
