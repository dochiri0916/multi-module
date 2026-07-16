package com.dochiri.security.adapter.in.web.authentication;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Component
@RequiredArgsConstructor
public class PublicApiRequestMatcher implements RequestMatcher {

    private final RequestMappingHandlerMapping handlerMapping;

    @Override
    public boolean matches(HttpServletRequest request) {
        try {
            HandlerExecutionChain handler = handlerMapping.getHandler(request);
            if (handler == null || !(handler.getHandler() instanceof HandlerMethod handlerMethod)) {
                return false;
            }
            return AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), PublicApi.class)
                    || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), PublicApi.class);
        } catch (Exception exception) {
            return false;
        }
    }
}
