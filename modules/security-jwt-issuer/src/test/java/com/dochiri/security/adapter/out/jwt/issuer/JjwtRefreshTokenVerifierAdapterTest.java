package com.dochiri.security.adapter.out.jwt.issuer;

import com.dochiri.security.adapter.out.jwt.issuer.configuration.JwtIssuerProperties;
import com.dochiri.security.application.exception.InvalidTokenException;
import com.dochiri.security.application.exception.SecurityApplicationErrorCode;
import com.dochiri.security.application.port.out.DecodedRefreshSessionToken;
import com.dochiri.security.application.port.out.DecodedRefreshToken;
import com.dochiri.security.domain.model.AuthenticationRole;
import com.dochiri.security.domain.model.AuthenticationSubject;
import com.dochiri.security.domain.model.EncodedToken;
import com.dochiri.security.domain.model.RefreshSessionId;
import com.dochiri.security.domain.model.TokenId;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JjwtRefreshTokenVerifierAdapterTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-characters-long";
    private static final String ROLE_CLAIM = "role";
    private static final String CATEGORY_CLAIM = "category";
    private static final String SESSION_ID_CLAIM = "sid";
    private static final AuthenticationSubject SUBJECT = new AuthenticationSubject("member-01");
    private static final AuthenticationRole ROLE = new AuthenticationRole("MEMBER");
    private static final TokenId TOKEN_ID = new TokenId("refresh-token-id");
    private static final RefreshSessionId SESSION_ID = new RefreshSessionId("refresh-session-id");
    private static final Instant NOW = Instant.parse("2029-01-01T00:00:00Z");
    private static final Instant EXPIRATION = Instant.parse("2030-01-01T00:00:00Z");

    private final JjwtRefreshTokenVerifierAdapter verifier = new JjwtRefreshTokenVerifierAdapter(
            new JwtIssuerProperties(SECRET, Duration.ofHours(1), Duration.ofDays(7)),
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    @DisplayName("유효한 Refresh Token과 세션 정보를 자체 타입으로 검증한다")
    void verifiesRefreshTokenAndSessionValuesWithOwnTypes() {
        // given
        EncodedToken token = refreshToken(builder -> builder.claim(SESSION_ID_CLAIM, SESSION_ID.value()));

        // when
        DecodedRefreshToken decoded = verifier.verifyRefresh(token);
        DecodedRefreshSessionToken session = verifier.verifyRefreshSession(token);

        // then
        assertThat(decoded.subject()).isEqualTo(SUBJECT);
        assertThat(decoded.role()).isEqualTo(ROLE);
        assertThat(decoded.tokenId()).isEqualTo(TOKEN_ID);
        assertThat(decoded.expiresAt().value()).isEqualTo(EXPIRATION);
        assertThat(session.sessionId()).isEqualTo(SESSION_ID);
    }

    @Test
    @DisplayName("Access Token을 Refresh Token으로 검증하면 category 오류로 거부한다")
    void rejectsAccessTokenCategoryAsRefreshToken() {
        // given
        EncodedToken token = signedToken(builder -> builder
                .subject(SUBJECT.value())
                .claim(ROLE_CLAIM, ROLE.value())
                .claim(CATEGORY_CLAIM, "access")
                .id(TOKEN_ID.value())
                .expiration(Date.from(EXPIRATION)));

        // when & then
        assertCode(token, SecurityApplicationErrorCode.TOKEN_CATEGORY_INVALID);
    }

    @Test
    @DisplayName("형식이 잘못된 Refresh Token을 Application 오류로 변환한다")
    void mapsMalformedRefreshTokenToApplicationError() {
        // given
        EncodedToken malformed = new EncodedToken("not-a-jwt");

        // when & then
        assertCode(malformed, SecurityApplicationErrorCode.TOKEN_MALFORMED);
    }

    @Test
    @DisplayName("만료된 Refresh Token을 전용 오류로 거부한다")
    void rejectsExpiredRefreshToken() {
        // given
        EncodedToken expired = refreshToken(builder ->
                builder.expiration(Date.from(Instant.parse("2020-01-01T00:00:00Z"))));

        // when & then
        assertCode(expired, SecurityApplicationErrorCode.TOKEN_EXPIRED);
    }

    @Test
    @DisplayName("subject가 없는 Refresh Token을 전용 오류로 거부한다")
    void rejectsRefreshTokenWithoutSubject() {
        // given
        EncodedToken token = signedToken(builder -> builder
                .claim(ROLE_CLAIM, ROLE.value())
                .claim(CATEGORY_CLAIM, "refresh")
                .id(TOKEN_ID.value())
                .expiration(Date.from(EXPIRATION)));

        // when & then
        assertCode(token, SecurityApplicationErrorCode.TOKEN_SUBJECT_MISSING);
    }

    @Test
    @DisplayName("role이 없는 Refresh Token을 전용 오류로 거부한다")
    void rejectsRefreshTokenWithoutRole() {
        // given
        EncodedToken token = signedToken(builder -> builder
                .subject(SUBJECT.value())
                .claim(CATEGORY_CLAIM, "refresh")
                .id(TOKEN_ID.value())
                .expiration(Date.from(EXPIRATION)));

        // when & then
        assertCode(token, SecurityApplicationErrorCode.TOKEN_ROLE_MISSING);
    }

    @Test
    @DisplayName("jti가 없는 Refresh Token을 전용 오류로 거부한다")
    void rejectsRefreshTokenWithoutTokenId() {
        // given
        EncodedToken token = signedToken(builder -> builder
                .subject(SUBJECT.value())
                .claim(ROLE_CLAIM, ROLE.value())
                .claim(CATEGORY_CLAIM, "refresh")
                .expiration(Date.from(EXPIRATION)));

        // when & then
        assertCode(token, SecurityApplicationErrorCode.TOKEN_ID_MISSING);
    }

    @Test
    @DisplayName("만료 시각이 없는 Refresh Token을 전용 오류로 거부한다")
    void rejectsRefreshTokenWithoutExpiration() {
        // given
        EncodedToken token = signedToken(builder -> builder
                .subject(SUBJECT.value())
                .claim(ROLE_CLAIM, ROLE.value())
                .claim(CATEGORY_CLAIM, "refresh")
                .id(TOKEN_ID.value()));

        // when & then
        assertCode(token, SecurityApplicationErrorCode.TOKEN_EXPIRATION_MISSING);
    }

    @Test
    @DisplayName("세션 식별자가 없는 Refresh Token은 회전 검증을 거부한다")
    void rejectsRefreshTokenWithoutSessionIdForRotation() {
        // given
        EncodedToken token = refreshToken(builder -> builder.header().and());

        // when & then
        assertThatThrownBy(() -> verifier.verifyRefreshSession(token))
                .isInstanceOfSatisfying(InvalidTokenException.class, exception ->
                        assertThat(exception.code())
                                .isEqualTo(SecurityApplicationErrorCode.REFRESH_SESSION_ID_MISSING));
    }

    private void assertCode(EncodedToken token, SecurityApplicationErrorCode expectedCode) {
        assertThatThrownBy(() -> verifier.verifyRefresh(token))
                .isInstanceOfSatisfying(InvalidTokenException.class, exception ->
                        assertThat(exception.code()).isEqualTo(expectedCode));
    }

    private static EncodedToken refreshToken(Consumer<JwtBuilder> customizer) {
        return signedToken(builder -> {
            builder.subject(SUBJECT.value())
                    .claim(ROLE_CLAIM, ROLE.value())
                    .claim(CATEGORY_CLAIM, "refresh")
                    .id(TOKEN_ID.value())
                    .expiration(Date.from(EXPIRATION));
            customizer.accept(builder);
        });
    }

    private static EncodedToken signedToken(Consumer<JwtBuilder> customizer) {
        JwtBuilder builder = Jwts.builder();
        customizer.accept(builder);
        return new EncodedToken(builder
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact());
    }
}
