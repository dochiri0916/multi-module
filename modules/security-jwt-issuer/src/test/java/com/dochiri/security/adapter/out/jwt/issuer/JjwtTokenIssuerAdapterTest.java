package com.dochiri.security.adapter.out.jwt.issuer;

import com.dochiri.security.adapter.out.jwt.issuer.configuration.JwtIssuerProperties;
import com.dochiri.security.application.port.out.IssuedTokenPair;
import com.dochiri.security.domain.model.AuthenticationRole;
import com.dochiri.security.domain.model.AuthenticationSubject;
import com.dochiri.security.domain.model.CurrentTime;
import com.dochiri.security.domain.model.RefreshSessionId;
import com.dochiri.security.domain.model.TokenExpiration;
import com.dochiri.security.domain.model.TokenId;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class JjwtTokenIssuerAdapterTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-characters-long";
    private static final JwtIssuerProperties PROPERTIES = new JwtIssuerProperties(
            SECRET,
            Duration.ofHours(1),
            Duration.ofDays(7)
    );
    private static final AuthenticationSubject SUBJECT = new AuthenticationSubject("member-01");
    private static final AuthenticationRole ROLE = new AuthenticationRole("MEMBER");
    private static final TokenId TOKEN_ID = new TokenId("refresh-token-id");
    private static final RefreshSessionId SESSION_ID = new RefreshSessionId("refresh-session-id");
    private static final CurrentTime ISSUED_AT = new CurrentTime(Instant.parse("2029-01-01T00:00:00Z"));
    private static final TokenExpiration ABSOLUTE_EXPIRATION =
            new TokenExpiration(Instant.parse("2030-01-01T00:00:00Z"));

    private final JjwtTokenIssuerAdapter issuer = new JjwtTokenIssuerAdapter(PROPERTIES);

    @Test
    @DisplayName("Access와 Refresh Token을 한 쌍으로 발급한다")
    void Access와_Refresh_Token을_한_쌍으로_발급한다() {
        // given
        CurrentTime issuedAt = ISSUED_AT;

        // when
        IssuedTokenPair issued = issuer.issue(SUBJECT, ROLE, TOKEN_ID, issuedAt);
        Claims accessClaims = parse(issued.accessToken().value());
        Claims refreshClaims = parse(issued.refreshToken().value());

        // then
        assertThat(accessClaims.getSubject()).isEqualTo(SUBJECT.value());
        assertThat(accessClaims.get("category", String.class)).isEqualTo("access");
        assertThat(refreshClaims.get("category", String.class)).isEqualTo("refresh");
        assertThat(refreshClaims.getId()).isEqualTo(TOKEN_ID.value());
        assertThat(refreshClaims.get("sid", String.class)).isEqualTo(TOKEN_ID.value());
        assertThat(issued.refreshTokenExpiresAt().value())
                .isEqualTo(ISSUED_AT.value().plus(PROPERTIES.refreshTokenTtl()));
    }

    @Test
    @DisplayName("Token 회전은 세션과 절대 만료를 유지하고 새 Refresh Token 식별자를 사용한다")
    void Token_회전은_세션과_절대_만료를_유지하고_새_Refresh_Token_식별자를_사용한다() {
        // given
        TokenId rotatedTokenId = new TokenId("rotated-token-id");

        // when
        IssuedTokenPair rotated = issuer.rotate(
                SUBJECT,
                ROLE,
                SESSION_ID,
                rotatedTokenId,
                ABSOLUTE_EXPIRATION,
                ISSUED_AT
        );
        Claims refreshClaims = parse(rotated.refreshToken().value());

        // then
        assertThat(refreshClaims.get("sid", String.class)).isEqualTo(SESSION_ID.value());
        assertThat(refreshClaims.getId()).isEqualTo(rotatedTokenId.value());
        assertThat(refreshClaims.getExpiration().toInstant()).isEqualTo(ABSOLUTE_EXPIRATION.value());
        assertThat(rotated.refreshTokenExpiresAt()).isEqualTo(ABSOLUTE_EXPIRATION);
    }

    @Test
    @DisplayName("JJWT 타입을 Token 발급 Adapter의 공개 계약에 노출하지 않는다")
    void JJWT_타입을_Token_발급_Adapter의_공개_계약에_노출하지_않는다() {
        // given
        Method[] publicMethods = JjwtTokenIssuerAdapter.class.getMethods();

        // when
        boolean jjwtTypeExposed = Arrays.stream(publicMethods)
                .flatMap(method -> Stream.concat(
                        Stream.of(method.getReturnType()),
                        Arrays.stream(method.getParameterTypes())
                ))
                .map(Class::getName)
                .anyMatch(typeName -> typeName.startsWith("io.jsonwebtoken"));

        // then
        assertThat(jjwtTypeExposed).isFalse();
    }

    private static Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
