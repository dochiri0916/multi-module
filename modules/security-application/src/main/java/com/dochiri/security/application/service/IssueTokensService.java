package com.dochiri.security.application.service;

import com.dochiri.security.application.exception.TokenCodecContractException;
import com.dochiri.security.application.port.in.IssueTokensCommand;
import com.dochiri.security.application.port.in.IssueTokensResult;
import com.dochiri.security.application.port.in.IssueTokensUseCase;
import com.dochiri.security.application.port.out.CurrentTimePort;
import com.dochiri.security.application.port.out.IssuedTokenPair;
import com.dochiri.security.application.port.out.RefreshSessionRepositoryPort;
import com.dochiri.security.application.port.out.TokenIssuerPort;
import com.dochiri.security.application.port.out.TokenIdGeneratorPort;
import com.dochiri.security.domain.model.CurrentTime;
import com.dochiri.security.domain.model.RefreshSession;
import com.dochiri.security.domain.model.RefreshSessionId;
import com.dochiri.security.domain.model.TokenId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class IssueTokensService implements IssueTokensUseCase {

    private final TokenIssuerPort tokenIssuerPort;
    private final RefreshSessionRepositoryPort refreshSessionRepositoryPort;
    private final TokenIdGeneratorPort tokenIdGeneratorPort;
    private final CurrentTimePort currentTimePort;

    @Override
    @Transactional
    public IssueTokensResult execute(IssueTokensCommand command) {
        TokenId refreshTokenId = tokenIdGeneratorPort.generate();
        CurrentTime issuedAt = currentTimePort.currentTime();
        IssuedTokenPair tokenPair = tokenIssuerPort.issue(
                command.subject(),
                command.role(),
                refreshTokenId,
                issuedAt
        );
        if (!refreshTokenId.equals(tokenPair.refreshTokenId())) {
            throw TokenCodecContractException.unexpectedTokenId(refreshTokenId);
        }

        refreshSessionRepositoryPort.save(RefreshSession.issue(
                new RefreshSessionId(tokenPair.refreshTokenId().value()),
                tokenPair.refreshTokenId(),
                command.subject(),
                command.role(),
                tokenPair.refreshTokenExpiresAt()
        ));
        return new IssueTokensResult(
                tokenPair.accessToken(),
                tokenPair.refreshToken(),
                tokenPair.refreshTokenExpiresAt()
        );
    }
}
