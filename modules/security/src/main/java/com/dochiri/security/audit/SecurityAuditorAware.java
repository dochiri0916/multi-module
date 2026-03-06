package com.dochiri.security.audit;

import com.dochiri.security.jwt.JwtPrincipal;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public class SecurityAuditorAware implements AuditorAware<Long> {

    private final Long systemUserId;

    public SecurityAuditorAware(Long systemUserId) {
        this.systemUserId = systemUserId;
    }

    @Override
    public Optional<Long> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.of(systemUserId);
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof JwtPrincipal jwtPrincipal) {
            return Optional.of(jwtPrincipal.userId());
        }

        return Optional.of(systemUserId);
    }
}
