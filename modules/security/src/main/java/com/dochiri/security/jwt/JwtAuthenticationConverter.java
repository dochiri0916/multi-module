package com.dochiri.security.jwt;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

@RequiredArgsConstructor
public class JwtAuthenticationConverter {

    private final JwtProvider jwtProvider;

    public UsernamePasswordAuthenticationToken convert(String token) {
        Claims claims = jwtProvider.parseAndValidate(token);

        if (!jwtProvider.isAccessToken(claims)) {
            throw new BadCredentialsException("인증에 사용할 수 없는 토큰입니다.");
        }

        Long userId = jwtProvider.extractUserId(claims);
        String role = jwtProvider.extractRole(claims);

        JwtPrincipal principal = new JwtPrincipal(userId, role);

        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }
}
