package com.dochiri.security.adapter.in.web.configuration;

import com.dochiri.errorhandling.global.error.ErrorHandlingAutoConfiguration;
import com.dochiri.security.adapter.in.web.error.SecurityErrorCodeMappingProvider;
import com.dochiri.security.adapter.in.web.error.SecurityErrorMessageProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;

@AutoConfiguration(before = ErrorHandlingAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import({
        SecurityErrorCodeMappingProvider.class,
        SecurityErrorMessageProvider.class
})
public class SecurityErrorMappingAutoConfiguration {
}
