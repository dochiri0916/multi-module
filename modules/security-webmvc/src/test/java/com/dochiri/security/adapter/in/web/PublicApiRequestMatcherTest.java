package com.dochiri.security.adapter.in.web;

import com.dochiri.security.adapter.in.web.authentication.PublicApi;
import com.dochiri.security.adapter.in.web.authentication.PublicApiRequestMatcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PublicApiRequestMatcherTest {

    private static final String HTTP_GET = "GET";

    @Test
    @DisplayName("PublicApi가 선언된 handler method만 공개 요청으로 판정한다")
    void PublicApi가_선언된_handler_method만_공개_요청으로_판정한다() throws Exception {
        // given
        RequestMappingHandlerMapping handlerMapping = mock(RequestMappingHandlerMapping.class);
        MockHttpServletRequest request = new MockHttpServletRequest(HTTP_GET, "/api/public");
        Method method = EndpointController.class.getDeclaredMethod("publicEndpoint");
        when(handlerMapping.getHandler(request)).thenReturn(
                new HandlerExecutionChain(new HandlerMethod(new EndpointController(), method))
        );
        PublicApiRequestMatcher matcher = new PublicApiRequestMatcher(handlerMapping);

        // when
        boolean publicApi = matcher.matches(request);

        // then
        assertThat(publicApi).isTrue();
    }

    @Test
    @DisplayName("PublicApi가 없는 handler method는 보호 요청으로 판정한다")
    void PublicApi가_없는_handler_method는_보호_요청으로_판정한다() throws Exception {
        // given
        RequestMappingHandlerMapping handlerMapping = mock(RequestMappingHandlerMapping.class);
        MockHttpServletRequest request = new MockHttpServletRequest(HTTP_GET, "/api/private");
        Method method = EndpointController.class.getDeclaredMethod("privateEndpoint");
        when(handlerMapping.getHandler(request)).thenReturn(
                new HandlerExecutionChain(new HandlerMethod(new EndpointController(), method))
        );
        PublicApiRequestMatcher matcher = new PublicApiRequestMatcher(handlerMapping);

        // when
        boolean publicApi = matcher.matches(request);

        // then
        assertThat(publicApi).isFalse();
    }

    @Test
    @DisplayName("PublicApi가 선언된 controller의 handler method를 공개 요청으로 판정한다")
    void PublicApi가_선언된_controller의_handler_method를_공개_요청으로_판정한다() throws Exception {
        // given
        RequestMappingHandlerMapping handlerMapping = mock(RequestMappingHandlerMapping.class);
        MockHttpServletRequest request = new MockHttpServletRequest(HTTP_GET, "/api/class-public");
        Method method = PublicEndpointController.class.getDeclaredMethod("endpoint");
        when(handlerMapping.getHandler(request)).thenReturn(
                new HandlerExecutionChain(new HandlerMethod(new PublicEndpointController(), method))
        );
        PublicApiRequestMatcher matcher = new PublicApiRequestMatcher(handlerMapping);

        // when
        boolean publicApi = matcher.matches(request);

        // then
        assertThat(publicApi).isTrue();
    }

    @Test
    @DisplayName("handler를 찾지 못한 요청은 보호 요청으로 판정한다")
    void handler를_찾지_못한_요청은_보호_요청으로_판정한다() throws Exception {
        // given
        RequestMappingHandlerMapping handlerMapping = mock(RequestMappingHandlerMapping.class);
        MockHttpServletRequest request = new MockHttpServletRequest(HTTP_GET, "/missing");
        when(handlerMapping.getHandler(request)).thenReturn(null);
        PublicApiRequestMatcher matcher = new PublicApiRequestMatcher(handlerMapping);

        // when
        boolean publicApi = matcher.matches(request);

        // then
        assertThat(publicApi).isFalse();
    }

    @Test
    @DisplayName("HandlerMethod가 아닌 handler는 보호 요청으로 판정한다")
    void HandlerMethod가_아닌_handler는_보호_요청으로_판정한다() throws Exception {
        // given
        RequestMappingHandlerMapping handlerMapping = mock(RequestMappingHandlerMapping.class);
        MockHttpServletRequest request = new MockHttpServletRequest(HTTP_GET, "/resource");
        when(handlerMapping.getHandler(request)).thenReturn(new HandlerExecutionChain(new Object()));
        PublicApiRequestMatcher matcher = new PublicApiRequestMatcher(handlerMapping);

        // when
        boolean publicApi = matcher.matches(request);

        // then
        assertThat(publicApi).isFalse();
    }

    @Test
    @DisplayName("handler 조회 실패는 공개 권한으로 승격하지 않는다")
    void handler_조회_실패는_공개_권한으로_승격하지_않는다() throws Exception {
        // given
        RequestMappingHandlerMapping handlerMapping = mock(RequestMappingHandlerMapping.class);
        MockHttpServletRequest request = new MockHttpServletRequest(HTTP_GET, "/failure");
        when(handlerMapping.getHandler(request)).thenThrow(new Exception("mapping failure"));
        PublicApiRequestMatcher matcher = new PublicApiRequestMatcher(handlerMapping);

        // when
        boolean publicApi = matcher.matches(request);

        // then
        assertThat(publicApi).isFalse();
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
