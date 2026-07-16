package com.dochiri.security.application.service;

import com.dochiri.security.application.exception.SecurityApplicationErrorCode;
import com.dochiri.security.application.exception.TokenCodecContractException;
import com.dochiri.security.application.port.in.IssueTokensCommand;
import com.dochiri.security.application.port.in.IssueTokensResult;
import com.dochiri.security.application.port.out.IssuedTokenPair;
import com.dochiri.security.application.port.out.TokenIssuerPort;
import com.dochiri.security.domain.model.AuthenticationRole;
import com.dochiri.security.domain.model.AuthenticationSubject;
import com.dochiri.security.domain.model.CurrentTime;
import com.dochiri.security.domain.model.RefreshSession;
import com.dochiri.security.domain.model.TokenId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.dochiri.security.application.service.SecurityApplicationTestFixture.ACCESS_TOKEN;
import static com.dochiri.security.application.service.SecurityApplicationTestFixture.EXPIRATION;
import static com.dochiri.security.application.service.SecurityApplicationTestFixture.REFRESH_TOKEN;
import static com.dochiri.security.application.service.SecurityApplicationTestFixture.ROLE;
import static com.dochiri.security.application.service.SecurityApplicationTestFixture.SESSION_ID;
import static com.dochiri.security.application.service.SecurityApplicationTestFixture.SUBJECT;
import static com.dochiri.security.application.service.SecurityApplicationTestFixture.TOKEN_ID;
import static com.dochiri.security.application.service.SecurityApplicationTestFixture.fixedCurrentTime;
import static com.dochiri.security.application.service.SecurityApplicationTestFixture.fixedTokenIdGenerator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IssueTokensServiceTest {

    @Test
    @DisplayName("토큰 쌍을 발급하고 role 스냅샷을 가진 리프레시 세션 Aggregate를 저장한다")
    void issuesTokenPairAndStoresRefreshSessionRoleSnapshot() {
        // given
        SecurityApplicationTestFixture.FixedTokenIssuer issuer =
                new SecurityApplicationTestFixture.FixedTokenIssuer();
        SecurityApplicationTestFixture.InMemoryRefreshSessionRepository repository =
                new SecurityApplicationTestFixture.InMemoryRefreshSessionRepository();
        IssueTokensService service = new IssueTokensService(
                issuer,
                repository,
                fixedTokenIdGenerator(),
                fixedCurrentTime()
        );
        IssueTokensCommand command = new IssueTokensCommand(SUBJECT, ROLE);

        // when
        IssueTokensResult result = service.execute(command);

        // then
        assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(result.refreshToken()).isEqualTo(REFRESH_TOKEN);
        assertThat(result.refreshTokenExpiresAt()).isEqualTo(EXPIRATION);
        assertThat(repository.findByCurrentTokenId(TOKEN_ID))
                .contains(RefreshSession.issue(SESSION_ID, TOKEN_ID, SUBJECT, ROLE, EXPIRATION));
    }

    @Test
    @DisplayName("codec이 요청한 식별자와 다른 리프레시 토큰을 발급하면 저장하지 않고 거부한다")
    void rejectsTokenCodecReturningDifferentRefreshTokenId() {
        // given
        SecurityApplicationTestFixture.InMemoryRefreshSessionRepository repository =
                new SecurityApplicationTestFixture.InMemoryRefreshSessionRepository();
        TokenId issuedTokenId = new TokenId("different-token-id");
        IssueTokensService service = new IssueTokensService(
                tokenCodecReturning(issuedTokenId),
                repository,
                fixedTokenIdGenerator(),
                fixedCurrentTime()
        );

        // when & then
        assertThatThrownBy(() -> service.execute(new IssueTokensCommand(SUBJECT, ROLE)))
                .isInstanceOfSatisfying(TokenCodecContractException.class, exception -> {
                    assertThat(exception.code())
                            .isEqualTo(SecurityApplicationErrorCode.TOKEN_CODEC_CONTRACT_VIOLATION);
                    assertThat(exception.tokenId()).isEqualTo(TOKEN_ID);
                    assertThat(repository.findBySessionIdForUpdate(SESSION_ID)).isEmpty();
                });
    }

    private static TokenIssuerPort tokenCodecReturning(TokenId issuedTokenId) {
        return new TokenIssuerPort() {
            @Override
            public IssuedTokenPair issue(
                    AuthenticationSubject subject,
                    AuthenticationRole role,
                    TokenId refreshTokenId,
                    CurrentTime issuedAt
            ) {
                return new IssuedTokenPair(ACCESS_TOKEN, REFRESH_TOKEN, issuedTokenId, EXPIRATION);
            }
        };
    }
}
