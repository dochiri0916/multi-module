package com.dochiri.security.jpa.configuration;

import com.dochiri.security.jpa.entity.RefreshToken;
import com.dochiri.security.jpa.repository.RefreshTokenRepository;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

class SecurityJpaPackageRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(
            AnnotationMetadata importingClassMetadata,
            BeanDefinitionRegistry registry
    ) {
        AutoConfigurationPackages.register(
                registry,
                RefreshToken.class.getPackageName(),
                RefreshTokenRepository.class.getPackageName()
        );
    }

}