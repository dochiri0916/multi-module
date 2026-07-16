package com.dochiri.security.application.port.in;

@FunctionalInterface
public interface RevokeAllRefreshTokensUseCase {

    RevokeAllRefreshTokensResult execute(RevokeAllRefreshTokensCommand command);
}
