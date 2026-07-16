package com.dochiri.security.domain.model;

import com.dochiri.security.domain.exception.InvalidAuthenticationRoleException;
import com.dochiri.security.domain.exception.InvalidCurrentTimeException;
import com.dochiri.security.domain.exception.InvalidEncodedTokenException;
import com.dochiri.security.domain.exception.InvalidRevokedAtException;
import com.dochiri.security.domain.exception.InvalidTokenExpirationException;
import com.dochiri.security.domain.exception.InvalidTokenIdException;
import com.dochiri.security.domain.exception.SecurityDomainErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityValueObjectTest {

    private static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");

    @Test
    @DisplayName("인증 역할의 prefix와 앞뒤 공백을 제거하고 권한 형식으로 복원한다")
    void normalizesRoleAndRestoresGrantedAuthority() {
        // given
        String value = "  ROLE_ADMIN  ";

        // when
        AuthenticationRole role = new AuthenticationRole(value);

        // then
        assertThat(role)
                .extracting(AuthenticationRole::value, AuthenticationRole::grantedAuthority)
                .containsExactly("ADMIN", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("인증 역할이 null이면 전용 오류 코드로 거부한다")
    void rejectsNullAuthenticationRoleWithDedicatedErrorCode() {
        // given
        String value = missingValue();

        // when & then
        assertThatThrownBy(() -> new AuthenticationRole(value))
                .isInstanceOfSatisfying(InvalidAuthenticationRoleException.class, exception ->
                        assertThat(exception.code()).isEqualTo(SecurityDomainErrorCode.AUTHENTICATION_ROLE_REQUIRED));
    }

    @Test
    @DisplayName("prefix만 있는 인증 역할은 blank 오류 코드로 거부한다")
    void rejectsRoleContainingOnlyPrefixWithBlankErrorCode() {
        // given
        String value = " ROLE_ ";

        // when & then
        assertThatThrownBy(() -> new AuthenticationRole(value))
                .isInstanceOfSatisfying(InvalidAuthenticationRoleException.class, exception ->
                        assertThat(exception.code()).isEqualTo(SecurityDomainErrorCode.AUTHENTICATION_ROLE_BLANK));
    }

    @Test
    @DisplayName("인코딩 토큰을 정규화하되 문자열 표현에서는 원문을 숨긴다")
    void normalizesEncodedTokenAndRedactsToString() {
        // given
        String value = "  sensitive-token  ";

        // when
        EncodedToken token = new EncodedToken(value);

        // then
        assertThat(token)
                .extracting(EncodedToken::value, EncodedToken::toString)
                .containsExactly("sensitive-token", "EncodedToken[REDACTED]");
    }

    @Test
    @DisplayName("인코딩 토큰이 null이면 전용 오류 코드로 거부한다")
    void rejectsNullEncodedTokenWithDedicatedErrorCode() {
        // given
        String value = missingValue();

        // when & then
        assertThatThrownBy(() -> new EncodedToken(value))
                .isInstanceOfSatisfying(InvalidEncodedTokenException.class, exception ->
                        assertThat(exception.code()).isEqualTo(SecurityDomainErrorCode.ENCODED_TOKEN_REQUIRED));
    }

    @Test
    @DisplayName("인코딩 토큰이 blank이면 전용 오류 코드로 거부한다")
    void rejectsBlankEncodedTokenWithDedicatedErrorCode() {
        // given
        String value = "   ";

        // when & then
        assertThatThrownBy(() -> new EncodedToken(value))
                .isInstanceOfSatisfying(InvalidEncodedTokenException.class, exception ->
                        assertThat(exception.code()).isEqualTo(SecurityDomainErrorCode.ENCODED_TOKEN_BLANK));
    }

    @Test
    @DisplayName("토큰 식별자의 앞뒤 공백을 제거한다")
    void trimsSurroundingTokenIdentifierWhitespace() {
        // given
        String value = "  token-id-01  ";

        // when
        TokenId tokenId = new TokenId(value);

        // then
        assertThat(tokenId.value()).isEqualTo("token-id-01");
    }

    @Test
    @DisplayName("토큰 식별자가 null이면 전용 오류 코드로 거부한다")
    void rejectsNullTokenIdentifierWithDedicatedErrorCode() {
        // given
        String value = missingValue();

        // when & then
        assertThatThrownBy(() -> new TokenId(value))
                .isInstanceOfSatisfying(InvalidTokenIdException.class, exception ->
                        assertThat(exception.code()).isEqualTo(SecurityDomainErrorCode.TOKEN_ID_REQUIRED));
    }

    @Test
    @DisplayName("토큰 식별자가 blank이면 전용 오류 코드로 거부한다")
    void rejectsBlankTokenIdentifierWithDedicatedErrorCode() {
        // given
        String value = "   ";

        // when & then
        assertThatThrownBy(() -> new TokenId(value))
                .isInstanceOfSatisfying(InvalidTokenIdException.class, exception ->
                        assertThat(exception.code()).isEqualTo(SecurityDomainErrorCode.TOKEN_ID_BLANK));
    }

    @Test
    @DisplayName("현재 시각이 null이면 전용 오류 코드로 거부한다")
    void rejectsNullCurrentTimeWithDedicatedErrorCode() {
        // given
        Instant value = missingValue();

        // when & then
        assertThatThrownBy(() -> new CurrentTime(value))
                .isInstanceOfSatisfying(InvalidCurrentTimeException.class, exception ->
                        assertThat(exception.code()).isEqualTo(SecurityDomainErrorCode.CURRENT_TIME_REQUIRED));
    }

    @Test
    @DisplayName("토큰 만료 시각이 null이면 전용 오류 코드로 거부한다")
    void rejectsNullTokenExpirationWithDedicatedErrorCode() {
        // given
        Instant value = missingValue();

        // when & then
        assertThatThrownBy(() -> new TokenExpiration(value))
                .isInstanceOfSatisfying(InvalidTokenExpirationException.class, exception ->
                        assertThat(exception.code()).isEqualTo(SecurityDomainErrorCode.TOKEN_EXPIRATION_REQUIRED));
    }

    @Test
    @DisplayName("폐기 시각이 null이면 전용 오류 코드로 거부한다")
    void rejectsNullRevocationTimeWithDedicatedErrorCode() {
        // given
        Instant value = missingValue();

        // when & then
        assertThatThrownBy(() -> new RevokedAt(value))
                .isInstanceOfSatisfying(InvalidRevokedAtException.class, exception ->
                        assertThat(exception.code()).isEqualTo(SecurityDomainErrorCode.REVOKED_AT_REQUIRED));
    }

    @Test
    @DisplayName("만료 시각보다 앞선 현재 시각에는 토큰이 만료되지 않는다")
    void doesNotExpireTokenBeforeExpirationTime() {
        // given
        TokenExpiration expiration = new TokenExpiration(NOW.plusSeconds(1));
        CurrentTime currentTime = new CurrentTime(NOW);

        // when
        boolean expired = expiration.isExpiredAt(currentTime);

        // then
        assertThat(expired).isFalse();
    }

    @Test
    @DisplayName("만료 시각 경계에서는 토큰이 만료된다")
    void expiresTokenAtExpirationBoundary() {
        // given
        TokenExpiration expiration = new TokenExpiration(NOW);
        CurrentTime currentTime = new CurrentTime(NOW);

        // when
        boolean expired = expiration.isExpiredAt(currentTime);

        // then
        assertThat(expired).isTrue();
    }

    private static <T> T missingValue() {
        return new HashMap<String, T>().get("missing");
    }
}
