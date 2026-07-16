package com.dochiri.security.adapter.out.persistence.audit;

import com.dochiri.security.adapter.out.persistence.configuration.SecurityAuditProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Component
@ConditionalOnMissingBean(AuditorAware.class)
@RequiredArgsConstructor
public final class SecurityAuditorAware implements AuditorAware<String> {

    private final SecurityAuditProperties properties;

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.of(properties.systemSubject());
        }

        String authenticatedSubject = authentication.getName();
        if (authenticatedSubject == null || authenticatedSubject.isBlank()) {
            return Optional.of(properties.systemSubject());
        }
        return Optional.of(authenticatedSubject);
    }
}
