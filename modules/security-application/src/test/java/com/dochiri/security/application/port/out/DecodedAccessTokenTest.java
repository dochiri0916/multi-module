package com.dochiri.security.application.port.out;

import com.dochiri.security.domain.model.AuthenticationRole;
import com.dochiri.security.domain.model.AuthenticationSubject;
import com.dochiri.security.domain.model.TokenExpiration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DecodedAccessTokenTest {

    @Test
    @DisplayName("검증된 Access Token의 주체와 권한과 만료 시각을 보존한다")
    void preservesDecodedAccessTokenValues() {
        // given
        AuthenticationSubject subject = new AuthenticationSubject("member-01");
        AuthenticationRole role = new AuthenticationRole("MEMBER");
        TokenExpiration expiration = new TokenExpiration(Instant.parse("2030-01-01T00:00:00Z"));

        // when
        DecodedAccessToken token = new DecodedAccessToken(subject, role, expiration);

        // then
        assertThat(token.subject()).isEqualTo(subject);
        assertThat(token.role()).isEqualTo(role);
        assertThat(token.expiresAt()).isEqualTo(expiration);
    }
}
