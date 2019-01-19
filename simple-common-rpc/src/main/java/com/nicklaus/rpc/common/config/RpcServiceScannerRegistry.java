package com.nicklaus.rpc.common.config;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.reflect.Reflection;
import com.nicklaus.rpc.client.RpcClient;
import com.nicklaus.rpc.common.annotation.RpcService;
import com.nicklaus.rpc.common.annotation.RpcServiceScanner;
import com.nicklaus.rpc.common.proxy.RpcServiceProxy;

/**
 * scanner register
 *
 * @author weishibai
 * @date 2019/01/03 7:53 PM
 * 手动扫描 @GrpcService 注解的接口，生成动态代理类，注入到 Spring 容器
 */
public class RpcServiceScannerRegistry implements BeanFactoryAware, ImportBeanDefinitionRegistrar, ResourceLoaderAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcServiceScannerRegistry.class);

    private BeanFactory beanFactory;

    private ResourceLoader resourceLoader;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        ClassPathBeanDefinitionScanner scanner = new ClassPathRpcServiceScanner(registry);
        scanner.setResourceLoader(this.resourceLoader);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RpcService.class));
        Set<BeanDefinition> beanDefinitions = scanPackages(importingClassMetadata, scanner);

        for (BeanDefinition beanDefinition : beanDefinitions) {
            String className = beanDefinition.getBeanClassName();
            if (StringUtils.isBlank(className)) {
                continue;
            }
            try {
                // 创建代理类
                Class<?> target = Class.forName(className);
                RpcClient rpcClient = beanFactory.getBean(RpcClient.class);
                Object proxy = Reflection.newProxy(target, new RpcServiceProxy<>(target, rpcClient, new Object()));

                // 注册到 Spring 容器
                ((DefaultListableBeanFactory) beanFactory).registerSingleton(toCamelCase(className), proxy);
            } catch (ClassNotFoundException e) {
                LOGGER.warn("class not found : {}", className);
            }
        }
    }

    /**
     * 包扫描
     */
    private Set<BeanDefinition> scanPackages(AnnotationMetadata importingClassMetadata, ClassPathBeanDefinitionScanner scanner) {
        List<String> packages = Lists.newArrayList();
        Map<String, Object> annotationAttributes = importingClassMetadata.getAnnotationAttributes(RpcServiceScanner.class.getCanonicalName());
        if (annotationAttributes != null) {
            String[] basePackages = (String[]) annotationAttributes.get("packages");
            if (basePackages.length > 0) {
                packages.addAll(Arrays.asList(basePackages));
            }
        }
        Set<BeanDefinition> beanDefinitions = Sets.newHashSet();
        if (CollectionUtils.isEmpty(packages)) {
            return beanDefinitions;
        }
        packages.forEach(pack -> beanDefinitions.addAll(scanner.findCandidateComponents(pack)));
        return beanDefinitions;
    }

    private static class ClassPathRpcServiceScanner extends ClassPathBeanDefinitionScanner {

        ClassPathRpcServiceScanner(BeanDefinitionRegistry registry) {
            super(registry, false);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
            return beanDefinition.getMetadata().isInterface() && beanDefinition.getMetadata().isIndependent();
        }

    }

    private static String toCamelCase(String str) {
        String name = ClassUtils.getShortCanonicalName(str);
        return StringUtils.lowerCase(String.valueOf(name.charAt(0))) + name.substring(1);
    }

}