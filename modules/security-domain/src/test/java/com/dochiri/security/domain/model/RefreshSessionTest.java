package com.dochiri.security.domain.model;

import com.dochiri.security.domain.exception.InvalidAuthenticationRoleException;
import com.dochiri.security.domain.exception.InvalidAuthenticationSubjectException;
import com.dochiri.security.domain.exception.InvalidCurrentTimeException;
import com.dochiri.security.domain.exception.InvalidRefreshSessionIdException;
import com.dochiri.security.domain.exception.InvalidRefreshSessionStateException;
import com.dochiri.security.domain.exception.InvalidTokenExpirationException;
import com.dochiri.security.domain.exception.InvalidTokenIdException;
import com.dochiri.security.domain.exception.RefreshTokenReplayDetectedException;
import com.dochiri.security.domain.exception.SecurityDomainErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefreshSessionTest {

    private static final RefreshSessionId SESSION_ID = new RefreshSessionId("session-id-01");
    private static final TokenId TOKEN_ID = new TokenId("token-id-01");
    private static final TokenId ROTATED_TOKEN_ID = new TokenId("token-id-02");
    private static final AuthenticationSubject SUBJECT = new AuthenticationSubject("member-01");
    private static final AuthenticationRole ROLE = new AuthenticationRole("MEMBER");
    private static final TokenExpiration EXPIRATION =
            new TokenExpiration(Instant.parse("2030-01-01T00:00:00Z"));
    private static final Instant ACTIVE_INSTANT = Instant.parse("2029-01-01T00:00:00Z");
    private static final CurrentTime ACTIVE_TIME =
            new CurrentTime(ACTIVE_INSTANT);

    @Test
    @DisplayName("리프레시 세션을 발급하면 활성 상태와 role 스냅샷을 보관한다")
    void issuesActiveSessionWithRoleSnapshot() {
        // given
        RefreshSessionStatus expectedStatus = RefreshSessionStatus.ACTIVE;

        // when
        RefreshSession session = issueSession();

        // then
        assertThat(session.sessionId()).isEqualTo(SESSION_ID);
        assertThat(session.currentTokenId()).isEqualTo(TOKEN_ID);
        assertThat(session.subject()).isEqualTo(SUBJECT);
        assertThat(session.role()).isEqualTo(ROLE);
        assertThat(session.expiresAt()).isEqualTo(EXPIRATION);
        assertThat(session.status()).isEqualTo(expectedStatus);
        assertThat(session.revokedAt()).isNull();
    }

    @Test
    @DisplayName("세션 식별자가 없으면 리프레시 세션을 발급할 수 없다")
    void rejectsMissingSessionIdentifierWhenIssuing() {
        // given
        RefreshSessionId missingSessionId = missingValue();

        // when & then
        assertThatThrownBy(() -> RefreshSession.issue(missingSessionId, TOKEN_ID, SUBJECT, ROLE, EXPIRATION))
                .isInstanceOfSatisfying(InvalidRefreshSessionIdException.class, exception ->
                        assertThat(exception.code())
                                .isEqualTo(SecurityDomainErrorCode.REFRESH_SESSION_ID_REQUIRED)
                );
    }

    @Test
    @DisplayName("현재 token 식별자가 없으면 리프레시 세션을 발급할 수 없다")
    void rejectsMissingCurrentTokenIdentifierWhenIssuing() {
        // given
        TokenId missingTokenId = missingValue();

        // when & then
        assertThatThrownBy(() -> RefreshSession.issue(SESSION_ID, missingTokenId, SUBJECT, ROLE, EXPIRATION))
                .isInstanceOfSatisfying(InvalidTokenIdException.class, exception ->
                        assertThat(exception.code()).isEqualTo(SecurityDomainErrorCode.TOKEN_ID_REQUIRED)
                );
    }

    @Test
    @DisplayName("인증 주체가 없으면 리프레시 세션을 발급할 수 없다")
    void rejectsMissingAuthenticationSubjectWhenIssuing() {
        // given
        AuthenticationSubject missingSubject = missingValue();

        // when & then
        assertThatThrownBy(() -> RefreshSession.issue(SESSION_ID, TOKEN_ID, missingSubject, ROLE, EXPIRATION))
                .isInstanceOfSatisfying(InvalidAuthenticationSubjectException.class, exception ->
                        assertThat(exception.code())
                                .isEqualTo(SecurityDomainErrorCode.AUTHENTICATION_SUBJECT_REQUIRED)
                );
    }

    @Test
    @DisplayName("인증 role이 없으면 리프레시 세션을 발급할 수 없다")
    void rejectsMissingAuthenticationRoleWhenIssuing() {
        // given
        AuthenticationRole missingRole = missingValue();

        // when & then
        assertThatThrownBy(() -> RefreshSession.issue(SESSION_ID, TOKEN_ID, SUBJECT, missingRole, EXPIRATION))
                .isInstanceOfSatisfying(InvalidAuthenticationRoleException.class, exception ->
                        assertThat(exception.code())
                                .isEqualTo(SecurityDomainErrorCode.AUTHENTICATION_ROLE_REQUIRED)
                );
    }

    @Test
    @DisplayName("만료 시각이 없으면 리프레시 세션을 발급할 수 없다")
    void rejectsMissingExpirationWhenIssuing() {
        // given
        TokenExpiration missingExpiration = missingValue();

        // when & then
        assertThatThrownBy(() -> RefreshSession.issue(SESSION_ID, TOKEN_ID, SUBJECT, ROLE, missingExpiration))
                .isInstanceOfSatisfying(InvalidTokenExpirationException.class, exception ->
                        assertThat(exception.code()).isEqualTo(SecurityDomainErrorCode.TOKEN_EXPIRATION_REQUIRED)
                );
    }

    @Test
    @DisplayName("상태가 없으면 리프레시 세션을 복원할 수 없다")
    void rejectsMissingStatusWhenReconstituting() {
        // given
        RefreshSessionStatus missingStatus = missingValue();

        // when & then
        assertThatThrownBy(() -> RefreshSession.reconstitute(
                SESSION_ID,
                TOKEN_ID,
                SUBJECT,
                ROLE,
                EXPIRATION,
                missingStatus,
                null
        )).isInstanceOfSatisfying(InvalidRefreshSessionStateException.class, exception ->
                assertThat(exception.code()).isEqualTo(SecurityDomainErrorCode.REFRESH_SESSION_STATUS_REQUIRED)
        );
    }

    @Test
    @DisplayName("현재 token 식별자로 rotation하면 같은 세션의 token만 교체한다")
    void rotatesCurrentTokenIdentifierWithinSameSession() {
        // given
        RefreshSession session = issueSession();

        // when
        RefreshSession rotated = session.rotate(TOKEN_ID, ROTATED_TOKEN_ID, ACTIVE_TIME);

        // then
        assertThat(rotated.sessionId()).isEqualTo(SESSION_ID);
        assertThat(rotated.currentTokenId()).isEqualTo(ROTATED_TOKEN_ID);
        assertThat(rotated.subject()).isEqualTo(SUBJECT);
        assertThat(rotated.role()).isEqualTo(ROLE);
        assertThat(rotated.expiresAt()).isEqualTo(EXPIRATION);
        assertThat(rotated.status()).isEqualTo(RefreshSessionStatus.ACTIVE);
    }

    @Test
    @DisplayName("현재 token이 아닌 식별자로 rotation하면 재사용 탐지 예외를 던진다")
    void detectsReplayedTokenIdentifierDuringRotation() {
        // given
        RefreshSession session = issueSession().rotate(TOKEN_ID, ROTATED_TOKEN_ID, ACTIVE_TIME);
        TokenId replayedTokenId = TOKEN_ID;

        // when & then
        assertThatThrownBy(() -> assertThat(
                session.rotate(replayedTokenId, new TokenId("token-id-03"), ACTIVE_TIME)
        ).isNull())
                .isInstanceOfSatisfying(RefreshTokenReplayDetectedException.class, exception -> {
                    assertThat(exception.code()).isEqualTo(SecurityDomainErrorCode.REFRESH_TOKEN_REPLAYED);
                    assertThat(exception.sessionId()).isEqualTo(SESSION_ID);
                    assertThat(exception.presentedTokenId()).isEqualTo(replayedTokenId);
                    assertThat(exception.currentTokenId()).isEqualTo(ROTATED_TOKEN_ID);
                });
    }

    @Test
    @DisplayName("같은 token 식별자로 rotation하면 새 token 요구 오류로 거부한다")
    void rejectsUnchangedTokenIdentifierDuringRotation() {
        // given
        RefreshSession session = issueSession();

        // when & then
        assertThatThrownBy(() -> assertThat(session.rotate(TOKEN_ID, TOKEN_ID, ACTIVE_TIME)).isNull())
                .isInstanceOfSatisfying(InvalidRefreshSessionStateException.class, exception ->
                        assertThat(exception.code()).isEqualTo(
                                SecurityDomainErrorCode.REFRESH_TOKEN_ROTATION_ID_UNCHANGED
                        )
                );
    }

    @Test
    @DisplayName("rotation의 현재 token과 교체 token 식별자는 모두 필수다")
    void requiresBothTokenIdentifiersDuringRotation() {
        // given
        RefreshSession session = issueSession();

        // when & then
        assertThatThrownBy(() -> assertThat(
                session.rotate(missingValue(), ROTATED_TOKEN_ID, ACTIVE_TIME)
        ).isNull())
                .isInstanceOfSatisfying(InvalidTokenIdException.class, exception ->
                        assertThat(exception.code()).isEqualTo(SecurityDomainErrorCode.TOKEN_ID_REQUIRED)
                );
        assertThatThrownBy(() -> assertThat(
                session.rotate(TOKEN_ID, missingValue(), ACTIVE_TIME)
        ).isNull())
                .isInstanceOfSatisfying(InvalidTokenIdException.class, exception ->
                        assertThat(exception.code()).isEqualTo(SecurityDomainErrorCode.TOKEN_ID_REQUIRED)
                );
    }

    @Test
    @DisplayName("rotation 기준 시각이 없으면 리프레시 세션을 변경할 수 없다")
    void rejectsRotationWithoutCurrentTime() {
        // given
        RefreshSession session = issueSession();

        // when & then
        assertThatThrownBy(() -> assertThat(
                session.rotate(TOKEN_ID, ROTATED_TOKEN_ID, missingValue())
        ).isNull())
                .isInstanceOfSatisfying(InvalidCurrentTimeException.class, exception ->
                        assertThat(exception.code()).isEqualTo(SecurityDomainErrorCode.CURRENT_TIME_REQUIRED)
                );
    }

    @Test
    @DisplayName("폐기된 세션은 rotation할 수 없다")
    void rejectsRotationForRevokedSession() {
        // given
        RevokedAt revokedAt = new RevokedAt(Instant.parse("2028-12-31T00:00:00Z"));
        RefreshSession session = issueSession().revoke(revokedAt);

        // when & then
        assertThatThrownBy(() -> assertThat(
                session.rotate(TOKEN_ID, ROTATED_TOKEN_ID, ACTIVE_TIME)
        ).isNull())
                .isInstanceOfSatisfying(InvalidRefreshSessionStateException.class, exception -> {
                    assertThat(exception.code()).isEqualTo(SecurityDomainErrorCode.REFRESH_SESSION_INACTIVE);
                    assertThat(exception.sessionId()).isEqualTo(SESSION_ID);
                    assertThat(exception.status()).isEqualTo(RefreshSessionStatus.REVOKED);
                });
    }

    @Test
    @DisplayName("만료 경계에 도달한 세션은 rotation할 수 없다")
    void rejectsRotationAtExpirationBoundary() {
        // given
        RefreshSession session = issueSession();
        CurrentTime expirationBoundary = new CurrentTime(EXPIRATION.value());

        // when & then
        assertThatThrownBy(() -> assertThat(
                session.rotate(TOKEN_ID, ROTATED_TOKEN_ID, expirationBoundary)
        ).isNull())
                .isInstanceOfSatisfying(InvalidRefreshSessionStateException.class, exception -> {
                    assertThat(exception.code()).isEqualTo(SecurityDomainErrorCode.REFRESH_SESSION_INACTIVE);
                    assertThat(exception.sessionId()).isEqualTo(SESSION_ID);
                    assertThat(exception.status()).isEqualTo(RefreshSessionStatus.ACTIVE);
                });
    }

    @Test
    @DisplayName("활성 세션을 폐기하면 새 폐기 상태를 반환한다")
    void returnsNewRevokedSessionWhenActiveSessionIsRevoked() {
        // given
        RefreshSession session = issueSession();
        RevokedAt revokedAt = new RevokedAt(ACTIVE_INSTANT);

        // when
        RefreshSession revoked = session.revoke(revokedAt);

        // then
        assertThat(revoked).isNotSameAs(session);
        assertThat(revoked.status()).isEqualTo(RefreshSessionStatus.REVOKED);
        assertThat(revoked.revokedAt()).isEqualTo(revokedAt);
        assertThat(session.status()).isEqualTo(RefreshSessionStatus.ACTIVE);
    }

    @Test
    @DisplayName("활성 세션의 폐기 시각은 필수다")
    void requiresRevocationTimeWhenRevokingActiveSession() {
        // given
        RefreshSession session = issueSession();

        // when & then
        assertThatThrownBy(() -> assertThat(session.revoke(missingValue())).isNull())
                .isInstanceOfSatisfying(InvalidRefreshSessionStateException.class, exception -> {
                    assertThat(exception.code())
                            .isEqualTo(SecurityDomainErrorCode.REFRESH_SESSION_REVOKED_AT_REQUIRED);
                    assertThat(exception.status()).isEqualTo(RefreshSessionStatus.REVOKED);
                });
    }

    @Test
    @DisplayName("활성 여부를 판단할 기준 시각은 필수다")
    void requiresCurrentTimeWhenCheckingActivity() {
        // given
        RefreshSession session = issueSession();

        // when & then
        assertThatThrownBy(() -> session.isActiveAt(missingValue()))
                .isInstanceOfSatisfying(InvalidCurrentTimeException.class, exception ->
                        assertThat(exception.code()).isEqualTo(SecurityDomainErrorCode.CURRENT_TIME_REQUIRED)
                );
    }

    @Test
    @DisplayName("이미 폐기된 세션을 다시 폐기하면 같은 세션을 반환한다")
    void returnsSameSessionWhenRevokingAlreadyRevokedSession() {
        // given
        RefreshSession revoked = issueSession().revoke(
                new RevokedAt(ACTIVE_INSTANT)
        );

        // when
        RefreshSession result = revoked.revoke(new RevokedAt(Instant.parse("2029-01-02T00:00:00Z")));

        // then
        assertThat(result).isSameAs(revoked);
    }

    @Test
    @DisplayName("리프레시 세션의 동등성은 session 식별자만 사용한다")
    void comparesSessionsBySessionIdentifierOnly() {
        // given
        RefreshSession first = issueSession();
        RefreshSession second = RefreshSession.issue(
                SESSION_ID,
                ROTATED_TOKEN_ID,
                new AuthenticationSubject("member-02"),
                new AuthenticationRole("ADMIN"),
                new TokenExpiration(Instant.parse("2031-01-01T00:00:00Z"))
        );

        // when
        boolean equal = first.equals(second);

        // then
        assertThat(equal).isTrue();
        assertThat(first).isEqualTo(first);
        assertThat(first).isNotEqualTo("session-id-01");
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    @DisplayName("활성 상태를 복원할 때 폐기 시각이 있으면 거부한다")
    void rejectsActiveSessionWithRevocationTimeWhenReconstituting() {
        // given
        RevokedAt revokedAt = new RevokedAt(ACTIVE_INSTANT);

        // when & then
        assertThatThrownBy(() -> RefreshSession.reconstitute(
                SESSION_ID,
                TOKEN_ID,
                SUBJECT,
                ROLE,
                EXPIRATION,
                RefreshSessionStatus.ACTIVE,
                revokedAt
        )).isInstanceOfSatisfying(InvalidRefreshSessionStateException.class, exception ->
                assertThat(exception.code()).isEqualTo(SecurityDomainErrorCode.ACTIVE_SESSION_HAS_REVOKED_AT)
        );
    }

    @Test
    @DisplayName("폐기 상태를 복원할 때 폐기 시각이 없으면 거부한다")
    void rejectsRevokedSessionWithoutRevocationTimeWhenReconstituting() {
        // given
        RevokedAt missingRevokedAt = missingValue();

        // when & then
        assertThatThrownBy(() -> RefreshSession.reconstitute(
                SESSION_ID,
                TOKEN_ID,
                SUBJECT,
                ROLE,
                EXPIRATION,
                RefreshSessionStatus.REVOKED,
                missingRevokedAt
        )).isInstanceOfSatisfying(InvalidRefreshSessionStateException.class, exception ->
                assertThat(exception.code()).isEqualTo(SecurityDomainErrorCode.REFRESH_SESSION_REVOKED_AT_REQUIRED)
        );
    }

    private RefreshSession issueSession() {
        return RefreshSession.issue(SESSION_ID, TOKEN_ID, SUBJECT, ROLE, EXPIRATION);
    }

    private static <T> T missingValue() {
        return new HashMap<String, T>().get("missing");
    }
}
