package com.kwai.infra.grpc.common.config;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.kwai.infra.grpc.client.GrpcClientFacade;
import com.kwai.infra.grpc.common.annotation.GrpcService;
import com.kwai.infra.grpc.common.annotation.GrpcServiceScanner;
import com.kwai.infra.grpc.common.internal.InfraService;
import com.kwai.infra.grpc.server.GrpcServerFacade;
import com.kwai.infra.grpc.utils.GrpcProxyUtils;

/**
 * grpc auto configuration
 *
 * @author weishibai
 * @date 2018/12/31 12:36 PM
 */
@Configuration
@EnableConfigurationProperties(GrpcConfigurer.class)
public class GrpcAutoConfiguration {

    private final AbstractApplicationContext applicationContext;

    private final GrpcConfigurer grpcConfigurer;

    public GrpcAutoConfiguration(AbstractApplicationContext applicationContext, GrpcConfigurer configurer) {
        this.applicationContext = applicationContext;
        this.grpcConfigurer = configurer;
    }

    /**
     * PRC 服务调用
     */
    @Bean
    public InfraService infraService() {
        return new InfraService(applicationContext);
    }

    /**
     * RPC 服务端
     */
    @Bean
    @ConditionalOnMissingBean(GrpcServerFacade.class)
    @ConditionalOnProperty(value = "spring.grpc.enable", havingValue = "true")
    public GrpcServerFacade grpcServer(InfraService infraService) throws Exception {
        /* todo add server interceptors */
        GrpcServerFacade server = new GrpcServerFacade(infraService, grpcConfigurer);
        server.start();
        return server;
    }

    /**
     * RPC 客户端
     */
    @Bean
    @ConditionalOnMissingBean(GrpcClientFacade.class)
    public GrpcClientFacade grpcClient() {
        /* todo add client interceptors */
        if (CollectionUtils.isEmpty(grpcConfigurer.getRemoteServers())) {
            return null;
        }
        return new GrpcClientFacade(grpcConfigurer, null);
    }

    /**
     * 手动扫描 @GrpcService 注解的接口，生成动态代理类，注入到 Spring 容器
     */
    public static class ExternalGrpcServiceScannerRegister implements BeanFactoryAware, ImportBeanDefinitionRegistrar, ResourceLoaderAware {

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
            ClassPathBeanDefinitionScanner scanner = new ClassPathGrpcServiceScanner(registry);
            scanner.setResourceLoader(this.resourceLoader);
            scanner.addIncludeFilter(new AnnotationTypeFilter(GrpcService.class));
            Set<BeanDefinition> beanDefinitions = scanPackages(importingClassMetadata, scanner);
            GrpcProxyUtils.registerBeans(beanFactory, beanDefinitions);
        }

        /**
         * 包扫描
         */
        private Set<BeanDefinition> scanPackages(AnnotationMetadata importingClassMetadata, ClassPathBeanDefinitionScanner scanner) {
            List<String> packages = Lists.newArrayList();
            Map<String, Object> annotationAttributes = importingClassMetadata.getAnnotationAttributes(GrpcServiceScanner.class.getCanonicalName());
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

    }

    protected static class ClassPathGrpcServiceScanner extends ClassPathBeanDefinitionScanner {

        ClassPathGrpcServiceScanner(BeanDefinitionRegistry registry) {
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

}
