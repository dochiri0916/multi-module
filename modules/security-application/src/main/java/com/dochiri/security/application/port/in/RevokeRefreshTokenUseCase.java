package com.dochiri.security.application.port.in;

@FunctionalInterface
public interface RevokeRefreshTokenUseCase {

    RevokeRefreshTokenResult execute(RevokeRefreshTokenCommand command);
}
