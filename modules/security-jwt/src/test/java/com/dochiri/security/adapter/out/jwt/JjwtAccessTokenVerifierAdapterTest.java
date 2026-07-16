package com.dochiri.security.adapter.out.jwt;

import com.dochiri.security.adapter.out.jwt.configuration.JwtVerificationProperties;
import com.dochiri.security.application.exception.InvalidTokenException;
import com.dochiri.security.application.exception.SecurityApplicationErrorCode;
import com.dochiri.security.application.port.out.DecodedAccessToken;
import com.dochiri.security.domain.model.AuthenticationRole;
import com.dochiri.security.domain.model.AuthenticationSubject;
import com.dochiri.security.domain.model.EncodedToken;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Date;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JjwtAccessTokenVerifierAdapterTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-characters-long";
    private static final String ROLE_CLAIM = "role";
    private static final String CATEGORY_CLAIM = "category";
    private static final String ACCESS_CATEGORY = "access";
    private static final AuthenticationSubject SUBJECT = new AuthenticationSubject("member-01");
    private static final AuthenticationRole ROLE = new AuthenticationRole("MEMBER");
    private static final Instant NOW = Instant.parse("2029-01-01T00:00:00Z");
    private static final Instant EXPIRATION = Instant.parse("2030-01-01T00:00:00Z");

    private final JjwtAccessTokenVerifierAdapter verifier = new JjwtAccessTokenVerifierAdapter(
            new JwtVerificationProperties(SECRET),
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    @DisplayName("유효한 Access Token에서 인증 주체와 역할과 만료 시각을 검증한다")
    void verifiesSubjectRoleAndExpirationFromValidAccessToken() {
        // given
        EncodedToken token = accessToken();

        // when
        DecodedAccessToken decoded = verifier.verifyAccess(token);

        // then
        assertThat(decoded.subject()).isEqualTo(SUBJECT);
        assertThat(decoded.role()).isEqualTo(ROLE);
        assertThat(decoded.expiresAt().value()).isEqualTo(EXPIRATION);
    }

    @Test
    @DisplayName("Refresh Token을 Access Token으로 검증하면 category 오류로 거부한다")
    void rejectsRefreshTokenCategory() {
        // given
        EncodedToken token = signedToken(builder -> builder
                .subject(SUBJECT.value())
                .claim(ROLE_CLAIM, ROLE.value())
                .claim(CATEGORY_CLAIM, "refresh")
                .expiration(Date.from(EXPIRATION)));

        // when & then
        assertThatThrownBy(() -> verifier.verifyAccess(token))
                .isInstanceOfSatisfying(InvalidTokenException.class, exception ->
                        assertThat(exception.code()).isEqualTo(SecurityApplicationErrorCode.TOKEN_CATEGORY_INVALID));
    }

    @Test
    @DisplayName("형식이 잘못된 Token을 Application 오류로 변환한다")
    void mapsMalformedTokenToApplicationError() {
        // given
        EncodedToken malformed = new EncodedToken("not-a-jwt");

        // when & then
        assertThatThrownBy(() -> verifier.verifyAccess(malformed))
                .isInstanceOfSatisfying(InvalidTokenException.class, exception ->
                        assertThat(exception.code()).isEqualTo(SecurityApplicationErrorCode.TOKEN_MALFORMED));
    }

    @Test
    @DisplayName("만료된 Access Token을 전용 오류로 거부한다")
    void rejectsExpiredAccessToken() {
        // given
        EncodedToken expired = accessToken(builder ->
                builder.expiration(Date.from(Instant.parse("2020-01-01T00:00:00Z"))));

        // when & then
        assertThatThrownBy(() -> verifier.verifyAccess(expired))
                .isInstanceOfSatisfying(InvalidTokenException.class, exception ->
                        assertThat(exception.code()).isEqualTo(SecurityApplicationErrorCode.TOKEN_EXPIRED));
    }

    @Test
    @DisplayName("subject가 없는 Access Token을 전용 오류로 거부한다")
    void rejectsAccessTokenWithoutSubject() {
        // given
        EncodedToken token = signedToken(builder -> builder
                .claim(ROLE_CLAIM, ROLE.value())
                .claim(CATEGORY_CLAIM, ACCESS_CATEGORY)
                .expiration(Date.from(EXPIRATION)));

        // when & then
        assertThatThrownBy(() -> verifier.verifyAccess(token))
                .isInstanceOfSatisfying(InvalidTokenException.class, exception ->
                        assertThat(exception.code()).isEqualTo(SecurityApplicationErrorCode.TOKEN_SUBJECT_MISSING));
    }

    @Test
    @DisplayName("role이 없는 Access Token을 전용 오류로 거부한다")
    void rejectsAccessTokenWithoutRole() {
        // given
        EncodedToken token = signedToken(builder -> builder
                .subject(SUBJECT.value())
                .claim(CATEGORY_CLAIM, ACCESS_CATEGORY)
                .expiration(Date.from(EXPIRATION)));

        // when & then
        assertThatThrownBy(() -> verifier.verifyAccess(token))
                .isInstanceOfSatisfying(InvalidTokenException.class, exception ->
                        assertThat(exception.code()).isEqualTo(SecurityApplicationErrorCode.TOKEN_ROLE_MISSING));
    }

    @Test
    @DisplayName("만료 시각이 없는 Access Token을 전용 오류로 거부한다")
    void rejectsAccessTokenWithoutExpiration() {
        // given
        EncodedToken token = signedToken(builder -> builder
                .subject(SUBJECT.value())
                .claim(ROLE_CLAIM, ROLE.value())
                .claim(CATEGORY_CLAIM, ACCESS_CATEGORY));

        // when & then
        assertThatThrownBy(() -> verifier.verifyAccess(token))
                .isInstanceOfSatisfying(InvalidTokenException.class, exception ->
                        assertThat(exception.code())
                                .isEqualTo(SecurityApplicationErrorCode.TOKEN_EXPIRATION_MISSING));
    }

    @Test
    @DisplayName("JJWT 타입을 공개 메서드 계약에 노출하지 않는다")
    void hidesJjwtTypesFromPublicMethodContract() {
        // given
        Method[] publicMethods = JjwtAccessTokenVerifierAdapter.class.getMethods();

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

    private static EncodedToken accessToken(Consumer<JwtBuilder> customizer) {
        return signedToken(builder -> {
            builder.subject(SUBJECT.value())
                    .claim(ROLE_CLAIM, ROLE.value())
                    .claim(CATEGORY_CLAIM, ACCESS_CATEGORY)
                    .expiration(Date.from(EXPIRATION));
            customizer.accept(builder);
        });
    }

    private static EncodedToken accessToken() {
        return accessToken(builder -> builder.header().and());
    }

    private static EncodedToken signedToken(Consumer<JwtBuilder> customizer) {
        JwtBuilder builder = Jwts.builder();
        customizer.accept(builder);
        return new EncodedToken(builder
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact());
    }
}
