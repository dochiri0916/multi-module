package com.dochiri.security.adapter.in.web.authentication;

import com.dochiri.security.application.exception.InvalidTokenException;
import com.dochiri.security.domain.model.EncodedToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private static final String BEARER_TOKEN = "encoded-access-token";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Bearer 토큰이 유효하면 인증 정보를 SecurityContext에 저장한다")
    void storesAuthenticationForValidBearerToken() throws ServletException, IOException {
        // given
        JwtAuthenticationConverter converter = mock(JwtAuthenticationConverter.class);
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated("member", null, java.util.List.of());
        EncodedToken encodedToken = new EncodedToken(BEARER_TOKEN);
        when(converter.convert(encodedToken)).thenReturn(authentication);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(converter);
        MockHttpServletRequest request = requestWithAuthorization("Bearer " + BEARER_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        // when
        filter.doFilter(request, response, chain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(authentication);
        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("토큰이 없으면 인증 없이 필터 체인을 계속 실행한다")
    void continuesFilterChainWithoutAuthorizationHeader() throws ServletException, IOException {
        // given
        JwtAuthenticationConverter converter = mock(JwtAuthenticationConverter.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(converter);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        // when
        filter.doFilter(request, response, chain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
        verifyNoInteractions(converter);
    }

    @Test
    @DisplayName("지원하지 않는 Authorization 형식은 인증 토큰으로 해석하지 않는다")
    void ignoresUnsupportedAuthorizationFormat() throws ServletException, IOException {
        // given
        JwtAuthenticationConverter converter = mock(JwtAuthenticationConverter.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(converter);
        MockHttpServletRequest request = requestWithAuthorization("Basic credentials");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        // when
        filter.doFilter(request, response, chain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
        verifyNoInteractions(converter);
    }

    @Test
    @DisplayName("빈 Bearer 토큰은 인증 토큰으로 해석하지 않는다")
    void ignoresBlankBearerToken() throws ServletException, IOException {
        // given
        JwtAuthenticationConverter converter = mock(JwtAuthenticationConverter.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(converter);
        MockHttpServletRequest request = requestWithAuthorization("Bearer ");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        // when
        filter.doFilter(request, response, chain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
        verifyNoInteractions(converter);
    }

    @Test
    @DisplayName("검증 실패 토큰은 인증 정보를 비우고 필터 체인을 계속 실행한다")
    void clearsAuthenticationWhenTokenVerificationFails() throws ServletException, IOException {
        // given
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("old", null, java.util.List.of())
        );
        JwtAuthenticationConverter converter = mock(JwtAuthenticationConverter.class);
        EncodedToken encodedToken = new EncodedToken(BEARER_TOKEN);
        when(converter.convert(encodedToken)).thenThrow(InvalidTokenException.malformed());
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(converter);
        MockHttpServletRequest request = requestWithAuthorization("Bearer " + BEARER_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        // when
        filter.doFilter(request, response, chain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    private static MockHttpServletRequest requestWithAuthorization(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", authorization);
        return request;
    }
}
