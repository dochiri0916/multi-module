package com.dochiri.security.application.port.in;

@FunctionalInterface
public interface IssueTokensUseCase {

    IssueTokensResult execute(IssueTokensCommand command);
}
