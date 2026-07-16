package com.dochiri.security.adapter.in.web.authentication;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Component
@RequiredArgsConstructor
public class PublicApiRequestMatcher implements RequestMatcher {

    private final RequestMappingHandlerMapping handlerMapping;

    @Override
    public boolean matches(HttpServletRequest request) {
        HandlerMethod handlerMethod = handlerMethodFor(request);
        if (handlerMethod == null) {
            return false;
        }
        return AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), PublicApi.class)
                || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), PublicApi.class);
    }

    private HandlerMethod handlerMethodFor(HttpServletRequest request) {
        return handlerMapping.getHandlerMethods().entrySet().stream()
                .filter(entry -> entry.getKey().getMatchingCondition(request) != null)
                .max((left, right) -> left.getKey().compareTo(right.getKey(), request))
                .map(java.util.Map.Entry::getValue)
                .orElse(null);
    }
}
