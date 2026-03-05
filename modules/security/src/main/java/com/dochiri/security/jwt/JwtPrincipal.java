package com.dochiri.security.jwt;

public record JwtPrincipal(
        Long userId,
        String role
) {
}