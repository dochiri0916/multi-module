package com.dochiri.errorhandling.global.error;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ProblemDetail;
import org.springframework.web.servlet.DispatcherServlet;

import java.util.List;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({ProblemDetail.class, DispatcherServlet.class})
public class ErrorHandlingAutoConfiguration {

    @Bean
    GlobalErrorCodeMappingProvider globalErrorCodeMappingProvider() {
        return new GlobalErrorCodeMappingProvider();
    }

    @Bean
    GlobalErrorMessageProvider globalErrorMessageProvider() {
        return new GlobalErrorMessageProvider();
    }

    @Bean
    ApiExceptionMapper apiExceptionMapper(List<ErrorCodeMappingProvider> providers) {
        return new ApiExceptionMapper(providers);
    }

    @Bean
    ApiErrorMessageCatalog apiErrorMessageCatalog(List<ApiErrorMessageProvider> providers) {
        return new ApiErrorMessageCatalog(providers);
    }

    @Bean
    @Lazy(false)
    ApiErrorContractValidator apiErrorContractValidator(
            List<ErrorCodeMappingProvider> mappingProviders,
            List<ApiErrorMessageProvider> messageProviders
    ) {
        ApiErrorContractValidator validator = new ApiErrorContractValidator(mappingProviders, messageProviders);
        validator.validate();
        return validator;
    }

    @Bean
    @ConditionalOnMissingBean
    ApiProblemDetailFactory apiProblemDetailFactory() {
        return new ApiProblemDetailFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    GlobalExceptionHandler globalExceptionHandler(
            ApiProblemDetailFactory problemDetailFactory,
            ApiExceptionMapper exceptionMapper,
            ApiErrorMessageCatalog messageCatalog
    ) {
        return new GlobalExceptionHandler(problemDetailFactory, exceptionMapper, messageCatalog);
    }
}
