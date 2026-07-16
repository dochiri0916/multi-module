package com.dochiri.security.application.port.in;

@FunctionalInterface
public interface VerifyRefreshTokenUseCase {

    VerifyRefreshTokenResult execute(VerifyRefreshTokenQuery query);
}
