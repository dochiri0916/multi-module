package com.dochiri.security.adapter.in.web;

import com.dochiri.security.adapter.in.web.authentication.PublicApi;
import com.dochiri.security.adapter.in.web.authentication.PublicApiRequestMatcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.util.ServletRequestPathUtils;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PublicApiRequestMatcherTest {

    private static final String HTTP_GET = "GET";

    @Test
    @DisplayName("PublicApi가 선언된 handler method만 공개 요청으로 판정한다")
    void recognizesMethodLevelPublicApi() throws NoSuchMethodException {
        // given
        Method method = EndpointController.class.getDeclaredMethod("publicEndpoint");
        HandlerMethod handlerMethod = new HandlerMethod(new EndpointController(), method);
        PublicApiRequestMatcher matcher = matcher("/api/public", handlerMethod);

        // when
        boolean publicApi = matcher.matches(request("/api/public"));

        // then
        assertThat(publicApi).isTrue();
    }

    @Test
    @DisplayName("PublicApi가 없는 handler method는 보호 요청으로 판정한다")
    void recognizesMissingPublicApiAsProtected() throws NoSuchMethodException {
        // given
        Method method = EndpointController.class.getDeclaredMethod("privateEndpoint");
        HandlerMethod handlerMethod = new HandlerMethod(new EndpointController(), method);
        PublicApiRequestMatcher matcher = matcher("/api/private", handlerMethod);

        // when
        boolean publicApi = matcher.matches(request("/api/private"));

        // then
        assertThat(publicApi).isFalse();
    }

    @Test
    @DisplayName("PublicApi가 선언된 controller의 handler method를 공개 요청으로 판정한다")
    void recognizesTypeLevelPublicApi() throws NoSuchMethodException {
        // given
        Method method = PublicEndpointController.class.getDeclaredMethod("endpoint");
        HandlerMethod handlerMethod = new HandlerMethod(new PublicEndpointController(), method);
        PublicApiRequestMatcher matcher = matcher("/api/class-public", handlerMethod);

        // when
        boolean publicApi = matcher.matches(request("/api/class-public"));

        // then
        assertThat(publicApi).isTrue();
    }

    @Test
    @DisplayName("handler를 찾지 못한 요청은 보호 요청으로 판정한다")
    void recognizesMissingHandlerAsProtected() {
        // given
        RequestMappingHandlerMapping handlerMapping = mock(RequestMappingHandlerMapping.class);
        when(handlerMapping.getHandlerMethods()).thenReturn(Map.of());
        PublicApiRequestMatcher matcher = new PublicApiRequestMatcher(handlerMapping);

        // when
        boolean publicApi = matcher.matches(request("/missing"));

        // then
        assertThat(publicApi).isFalse();
    }

    @Test
    @DisplayName("일치하지 않는 handler mapping은 보호 요청으로 판정한다")
    void recognizesUnmatchedMappingAsProtected() throws NoSuchMethodException {
        // given
        Method method = EndpointController.class.getDeclaredMethod("publicEndpoint");
        HandlerMethod handlerMethod = new HandlerMethod(new EndpointController(), method);
        PublicApiRequestMatcher matcher = matcher("/api/public", handlerMethod);

        // when
        boolean publicApi = matcher.matches(request("/api/other"));

        // then
        assertThat(publicApi).isFalse();
    }

    private static PublicApiRequestMatcher matcher(
            String mappingPath,
            HandlerMethod handlerMethod
    ) {
        RequestMappingInfo mappingInfo = RequestMappingInfo.paths(mappingPath)
                .methods(RequestMethod.GET)
                .build();
        RequestMappingHandlerMapping handlerMapping = mock(RequestMappingHandlerMapping.class);
        when(handlerMapping.getHandlerMethods()).thenReturn(Map.of(mappingInfo, handlerMethod));
        return new PublicApiRequestMatcher(handlerMapping);
    }

    private static MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(HTTP_GET, path);
        ServletRequestPathUtils.parseAndCache(request);
        return request;
    }

    static class EndpointController {

        @PublicApi
        void publicEndpoint() {
        }

        void privateEndpoint() {
        }
    }

    @PublicApi
    static class PublicEndpointController {

        void endpoint() {
        }
    }
}
