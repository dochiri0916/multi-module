package com.dochiri.security.application.port.in;

@FunctionalInterface
public interface CleanupRefreshSessionsUseCase {

    CleanupRefreshSessionsResult execute(CleanupRefreshSessionsCommand command);
}
