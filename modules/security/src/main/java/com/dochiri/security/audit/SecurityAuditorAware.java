package com.dochiri.security.audit;

import com.dochiri.security.jwt.JwtPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@RequiredArgsConstructor
public class SecurityAuditorAware implements AuditorAware<String> {

    private final Long systemUserId;

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.of(systemUserId.toString());
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof JwtPrincipal jwtPrincipal) {
            return Optional.of(jwtPrincipal.userId().toString());
        }

        return Optional.of(systemUserId.toString());
    }

}
