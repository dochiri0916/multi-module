package com.dochiri.security.adapter.out.persistence.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@RequiredArgsConstructor
public final class SecurityAuditorAware implements AuditorAware<String> {

    private final String systemSubject;

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.of(systemSubject);
        }

        String authenticatedSubject = authentication.getName();
        if (authenticatedSubject == null || authenticatedSubject.isBlank()) {
            return Optional.of(systemSubject);
        }
        return Optional.of(authenticatedSubject);
    }
}
