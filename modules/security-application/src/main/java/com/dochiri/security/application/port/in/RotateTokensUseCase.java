package com.dochiri.security.application.port.in;

@FunctionalInterface
public interface RotateTokensUseCase {

    RotateTokensResult execute(RotateTokensCommand command);
}
