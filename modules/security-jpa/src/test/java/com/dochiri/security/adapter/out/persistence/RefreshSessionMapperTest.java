package com.dochiri.security.adapter.out.persistence;

import com.dochiri.security.domain.model.AuthenticationRole;
import com.dochiri.security.domain.model.AuthenticationSubject;
import com.dochiri.security.domain.model.RefreshSession;
import com.dochiri.security.domain.model.RefreshSessionId;
import com.dochiri.security.domain.model.RevokedAt;
import com.dochiri.security.domain.model.TokenExpiration;
import com.dochiri.security.domain.model.TokenId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshSessionMapperTest {

    @Test
    @DisplayName("JPA Entity와 순수 RefreshSession Aggregate를 손실 없이 변환한다")
    void JPA_Entity와_순수_RefreshSession_Aggregate를_손실_없이_변환한다() {
        // given
        RefreshSession domain = RefreshSession.issue(
                new RefreshSessionId("session-id"),
                new TokenId("token-id"),
                new AuthenticationSubject("member-string-id"),
                new AuthenticationRole("MEMBER"),
                new TokenExpiration(Instant.parse("2030-01-01T00:00:00Z"))
        ).revoke(new RevokedAt(Instant.parse("2029-01-01T00:00:00Z")));

        // when
        RefreshSessionEntity entity = RefreshSessionMapper.toEntity(domain);
        RefreshSession restored = RefreshSessionMapper.toDomain(entity);

        // then
        assertThat(restored.sessionId()).isEqualTo(domain.sessionId());
        assertThat(restored.currentTokenId()).isEqualTo(domain.currentTokenId());
        assertThat(restored.subject()).isEqualTo(domain.subject());
        assertThat(restored.role()).isEqualTo(domain.role());
        assertThat(restored.expiresAt()).isEqualTo(domain.expiresAt());
        assertThat(restored.status()).isEqualTo(domain.status());
        assertThat(restored.revokedAt()).isEqualTo(domain.revokedAt());
    }

    @Test
    @DisplayName("RefreshSessionEntity는 DB 기술 식별자와 version getter를 공개하지 않는다")
    void RefreshSessionEntity는_DB_기술_식별자와_version_getter를_공개하지_않는다() {
        // given
        Method[] publicMethods = RefreshSessionEntity.class.getMethods();

        // when
        boolean technicalGetterExposed = Arrays.stream(publicMethods)
                .map(Method::getName)
                .anyMatch(methodName -> "getId".equals(methodName) || "getVersion".equals(methodName));

        // then
        assertThat(technicalGetterExposed).isFalse();
    }
}
