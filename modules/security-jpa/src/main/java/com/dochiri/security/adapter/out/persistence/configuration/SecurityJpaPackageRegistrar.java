package com.dochiri.security.adapter.out.persistence.configuration;

import com.dochiri.security.adapter.out.persistence.RefreshSessionEntity;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

class SecurityJpaPackageRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(
            AnnotationMetadata importingClassMetadata,
            BeanDefinitionRegistry registry
    ) {
        AutoConfigurationPackages.register(registry, RefreshSessionEntity.class.getPackageName());
    }
}
