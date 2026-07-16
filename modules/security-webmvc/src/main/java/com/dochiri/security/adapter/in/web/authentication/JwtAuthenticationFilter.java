package com.dochiri.security.adapter.in.web.authentication;

import com.dochiri.security.application.exception.InvalidTokenException;
import com.dochiri.security.domain.model.EncodedToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtAuthenticationConverter authenticationConverter;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        EncodedToken token = resolveToken(request);
        if (token != null) {
            try {
                UsernamePasswordAuthenticationToken authentication = authenticationConverter.convert(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (InvalidTokenException exception) {
                log.debug("JWT 인증 실패: {}", exception.code());
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private EncodedToken resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION);
        if (!StringUtils.hasText(bearerToken) || !bearerToken.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String encodedToken = bearerToken.substring(BEARER_PREFIX.length());
        if (!StringUtils.hasText(encodedToken)) {
            return null;
        }
        return new EncodedToken(encodedToken);
    }
}
