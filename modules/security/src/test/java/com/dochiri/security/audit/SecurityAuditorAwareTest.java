package com.dochiri.security.audit;

import com.dochiri.security.jwt.JwtPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityAuditorAwareTest {

    private final SecurityAuditorAware auditorAware = new SecurityAuditorAware(0L);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 인증정보가_없으면_시스템_사용자_ID를_반환한다() {
        Optional<Long> auditor = auditorAware.getCurrentAuditor();

        assertThat(auditor).hasValue(0L);
    }

    @Test
    void JwtPrincipal_인증이면_해당_userId를_반환한다() {
        JwtPrincipal principal = new JwtPrincipal(42L, "USER");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<Long> auditor = auditorAware.getCurrentAuditor();

        assertThat(auditor).hasValue(42L);
    }

    @Test
    void 익명_인증이면_시스템_사용자_ID를_반환한다() {
        AnonymousAuthenticationToken auth = new AnonymousAuthenticationToken(
                "key", "anonymous", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<Long> auditor = auditorAware.getCurrentAuditor();

        assertThat(auditor).hasValue(0L);
    }

    @Test
    void JwtPrincipal이_아닌_다른_Principal이면_시스템_사용자_ID를_반환한다() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "stringPrincipal", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<Long> auditor = auditorAware.getCurrentAuditor();

        assertThat(auditor).hasValue(0L);
    }

    @Test
    void 커스텀_시스템_사용자_ID를_사용할_수_있다() {
        SecurityAuditorAware customAuditor = new SecurityAuditorAware(999L);

        Optional<Long> auditor = customAuditor.getCurrentAuditor();

        assertThat(auditor).hasValue(999L);
    }
}
