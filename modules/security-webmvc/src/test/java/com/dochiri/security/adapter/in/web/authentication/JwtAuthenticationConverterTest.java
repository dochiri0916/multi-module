package com.dochiri.security.adapter.in.web.authentication;

import com.dochiri.security.application.port.out.AccessTokenVerifierPort;
import com.dochiri.security.application.port.out.DecodedAccessToken;
import com.dochiri.security.domain.model.AuthenticationRole;
import com.dochiri.security.domain.model.AuthenticationSubject;
import com.dochiri.security.domain.model.EncodedToken;
import com.dochiri.security.domain.model.TokenExpiration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationConverterTest {

    @Test
    @DisplayName("검증된 access token을 문자열 subject의 인증 principal로 변환한다")
    void 검증된_access_token을_문자열_subject의_인증_principal로_변환한다() {
        // given
        AuthenticationSubject subject = new AuthenticationSubject("member-string-id");
        AuthenticationRole role = new AuthenticationRole("MEMBER");
        AccessTokenVerifierPort verifier = new FixedAccessTokenVerifier(subject, role);
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter(verifier);

        // when
        UsernamePasswordAuthenticationToken authentication = converter.convert(
                new EncodedToken("encoded-access-token")
        );

        // then
        assertThat(authentication.getPrincipal()).isEqualTo(new JwtPrincipal(subject, role));
        assertThat(authentication.getName()).isEqualTo(subject.value());
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_MEMBER");
    }

    private record FixedAccessTokenVerifier(
            AuthenticationSubject subject,
            AuthenticationRole role
    ) implements AccessTokenVerifierPort {

        @Override
        public DecodedAccessToken verifyAccess(EncodedToken accessToken) {
            return new DecodedAccessToken(
                    subject,
                    role,
                    new TokenExpiration(Instant.parse("2030-01-01T00:00:00Z"))
            );
        }
    }
}
