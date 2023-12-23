package com.zzh.remote.remoteclient.remote;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Author：zzh
 */
public class RemoteClientsRegistrar implements ImportBeanDefinitionRegistrar {

    protected ClassPathScanningCandidateComponentProvider getScanner() {
        return new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                return beanDefinition.getMetadata().isIndependent() && !beanDefinition.getMetadata().isAnnotation();
            }
        };
    }

    @Override
    public void registerBeanDefinitions(@NonNull AnnotationMetadata annotationMetadata, @NonNull BeanDefinitionRegistry registry) {
        Set<String> basePackages = getBasePackages(annotationMetadata);

        ClassPathScanningCandidateComponentProvider beanScanner = getScanner();
        beanScanner.addIncludeFilter(new AnnotationTypeFilter(RemoteClient.class));
        Set<BeanDefinition> candidateComponents = new LinkedHashSet<>();
        basePackages.forEach(basePackage -> candidateComponents.addAll(beanScanner.findCandidateComponents(basePackage)));

        for (BeanDefinition candidateComponent : candidateComponents) {
            if (candidateComponent instanceof AnnotatedBeanDefinition) {
                //Verify annotated class is an interface
                AnnotatedBeanDefinition beanDefinition = (AnnotatedBeanDefinition) candidateComponent;
                AnnotationMetadata beanDefinitionMetadata = beanDefinition.getMetadata();
                Assert.isTrue(beanDefinitionMetadata.isInterface(), "@RemoteClient can only be specified on an interface");

                registerRemoteClient(registry, beanDefinitionMetadata);
            }
        }
    }

    private void registerRemoteClient(BeanDefinitionRegistry registry, AnnotationMetadata beanDefinitionMetadata) {
        String className = beanDefinitionMetadata.getClassName();
        BeanDefinitionBuilder definitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(RemoteFactoryBean.class);
        definitionBuilder.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);

        AbstractBeanDefinition beanDefinition = definitionBuilder.getBeanDefinition();
        beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(className);

        BeanDefinitionHolder beanDefinitionHolder = new BeanDefinitionHolder(beanDefinition, className);
        BeanDefinitionReaderUtils.registerBeanDefinition(beanDefinitionHolder, registry);
    }

    private Set<String> getBasePackages(AnnotationMetadata annotationMetadata) {
        MergedAnnotation<EnableRemoteClients> mergedAnnotation = annotationMetadata.getAnnotations().get(EnableRemoteClients.class);
        Optional<String[]> basePackage = mergedAnnotation.getValue("basePackages", String[].class);
        Optional<String[]> value = mergedAnnotation.getValue("value", String[].class);

        String[] basePackages = basePackage.orElse(new String[]{});
        String[] values = value.orElse(new String[]{});

        Set<String> packages = Stream.of(basePackages, values)
                .flatMap(Arrays::stream)
                .collect(Collectors.toSet());

        if (packages.isEmpty()) {
            packages.add(ClassUtils.getPackageName(annotationMetadata.getClassName()));
        }

        return packages;
    }
}
