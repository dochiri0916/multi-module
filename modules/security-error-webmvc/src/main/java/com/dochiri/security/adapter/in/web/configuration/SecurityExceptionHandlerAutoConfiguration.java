package com.dochiri.security.adapter.in.web.configuration;

import com.dochiri.security.adapter.in.web.error.SecurityExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import(SecurityExceptionHandler.class)
public class SecurityExceptionHandlerAutoConfiguration {
}
