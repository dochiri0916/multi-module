package com.dochiri.security.adapter.out.persistence.audit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityAuditorAwareTest {

    private final SecurityAuditorAware auditorAware = new SecurityAuditorAware("system-subject");

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("인증 정보가 없으면 시스템 subject를 반환한다")
    void 인증_정보가_없으면_시스템_subject를_반환한다() {
        // given
        SecurityContextHolder.clearContext();

        // when
        Optional<String> auditor = auditorAware.getCurrentAuditor();

        // then
        assertThat(auditor).hasValue("system-subject");
    }

    @Test
    @DisplayName("인증되면 숫자 DB 키가 아닌 문자열 principal 이름을 반환한다")
    void 인증되면_숫자_DB_키가_아닌_문자열_principal_이름을_반환한다() {
        // given
        String subject = "550e8400-e29b-41d4-a716-446655440000";
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                subject,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // when
        Optional<String> auditor = auditorAware.getCurrentAuditor();

        // then
        assertThat(auditor).hasValue(subject);
    }

    @Test
    @DisplayName("익명 인증이면 시스템 subject를 반환한다")
    void 익명_인증이면_시스템_subject를_반환한다() {
        // given
        AnonymousAuthenticationToken authentication = new AnonymousAuthenticationToken(
                "key",
                "anonymous",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // when
        Optional<String> auditor = auditorAware.getCurrentAuditor();

        // then
        assertThat(auditor).hasValue("system-subject");
    }
}
