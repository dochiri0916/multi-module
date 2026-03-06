package com.dochiri.security.jwt;

import com.dochiri.security.configuration.properties.JwtProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-characters-long";

    private JwtProvider jwtProvider;
    private JwtAuthenticationFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(new JwtProperties(SECRET, 3600000L, 604800000L));
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter(jwtProvider);
        filter = new JwtAuthenticationFilter(converter);
        filterChain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @Test
    void 유효한_Bearer_토큰이_있으면_SecurityContext에_인증정보가_설정된다() throws ServletException, IOException {
        String token = jwtProvider.generateAccessToken(1L, "USER");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        JwtPrincipal principal = (JwtPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(principal.userId()).isEqualTo(1L);
        assertThat(principal.role()).isEqualTo("USER");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void Authorization_헤더가_없으면_SecurityContext는_비어있다() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void Bearer_접두사가_없으면_SecurityContext는_비어있다() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic some-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void 잘못된_토큰이면_SecurityContext가_클리어되고_필터_체인은_계속된다() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void 리프레시_토큰으로_인증하면_SecurityContext가_클리어된다() throws ServletException, IOException {
        String refreshToken = jwtProvider.generateRefreshToken(1L, "USER");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + refreshToken);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
