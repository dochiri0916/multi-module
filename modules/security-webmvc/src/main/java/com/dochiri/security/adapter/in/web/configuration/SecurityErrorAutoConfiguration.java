package com.dochiri.security.adapter.in.web.configuration;

import com.dochiri.errorhandling.ErrorHandlingAutoConfiguration;
import com.dochiri.security.adapter.in.web.error.JwtAccessDeniedHandler;
import com.dochiri.security.adapter.in.web.error.JwtAuthenticationEntryPoint;
import com.dochiri.security.adapter.in.web.error.SecurityProblemDetailResponseAdapter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;

@AutoConfiguration(
        after = SecurityErrorMappingAutoConfiguration.class,
        before = ErrorHandlingAutoConfiguration.class
)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import({
        SecurityProblemDetailResponseAdapter.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class
})
public class SecurityErrorAutoConfiguration {
}
