package com.dochiri.security.config;

import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserAuditorAware implements AuditorAware<Long> {

    private static final long SYSTEM_USER_ID = 0L;

    @Override
    public Optional<Long> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.of(SYSTEM_USER_ID);
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Long userId) {
            return Optional.of(userId);
        }

        if (principal instanceof String userIdString) {
            try {
                return Optional.of(Long.parseLong(userIdString));
            } catch (NumberFormatException ignored) {
                return Optional.of(SYSTEM_USER_ID);
            }
        }

        return Optional.of(SYSTEM_USER_ID);
    }
}
